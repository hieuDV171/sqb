import re
import math
import bisect
import unicodedata
from dataclasses import dataclass, field, asdict
from typing import List, Dict, Optional, Tuple, Set, Any

from .config import Config, NEG_INF, TRANSITIONS, map_relation, CORROBORATION_RISK
from .schemas import (
    QuestionAuditRequest, BtpropAuditResponse, ViolationDto, OptionDto
)
from .backend import OpenAIBackend, EvaluationError, InvalidDecompositionError, ContextLossError, logsumexp, canonical

# ===== CONTEXT ANCHORS (Neo ngữ cảnh mở rộng cho đa môn học) =====
_ANCHOR_PATTERNS = (
    r"`([^`\n]+)`",                           # Backtick code/keyword
    r"\$([^\$\n]+)\$",                        # Inline math LaTeX $...$
    r"\$\$([^\$]+)\$\$",                      # Block math LaTeX $$...$$
    r"\\\[(.*?)\\\]",                         # Block math \[...\]
    r"[A-Za-z_$][\w$.]*\([^()\n]*\)",          # Function call: console.log('A')
    r"</?[A-Za-z][\w-]*[^<>\n]*>",             # HTML/XML tags
    r"\b(?:SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|GROUP BY|ORDER BY)\b[^\n;]+", # SQL
    r"https?://[^\s,;)\"']+",                 # URL
)

def context_anchors(text: str) -> List[Set[str]]:
    anchors, seen = [], set()
    for pattern in _ANCHOR_PATTERNS:
        for match in re.finditer(pattern, text, re.IGNORECASE):
            val = match.group(0).strip("`").strip()
            if val.startswith(("http://", "https://")):
                val = val.rstrip(".,;:!?)]}'\"")
            if len(val) < 2 or val in seen:
                continue
            seen.add(val)
            anchors.append({val})
    return anchors

def check_decomposition_context(statement: str, parts: List[str]):
    anchors = context_anchors(statement)
    if not anchors or len(parts) < 2:
        return
    for index, part in enumerate(parts, 1):
        folded = unicodedata.normalize("NFC", part).casefold()
        if not any(v.casefold() in folded for variants in anchors for v in variants):
            raise ContextLossError(f"Mệnh đề con #{index} mất neo ngữ cảnh (mã/công thức/URL) của phát biểu gốc")

def with_question_context(content: Optional[str], statement: str) -> str:
    if not content:
        return statement
    anchors = context_anchors(content)
    if not anchors or all(any(v in statement for v in variants) for variants in anchors):
        return statement
    return f"Bối cảnh của câu hỏi: {content}\nPhát biểu cần đánh giá trong bối cảnh trên: {statement}"

# ===== BELIEF NODE & PROBABILISTIC PROPAGATION =====
@dataclass
class BeliefNode:
    statement: str
    confidence: float
    method: str = "leaf"
    children: List['BeliefNode'] = field(default_factory=list)
    relations: List[str] = field(default_factory=list)
    notes: List[str] = field(default_factory=list)
    judged_statement: str = ""

def emission(node: BeliefNode, cfg: Config) -> List[float]:
    if not isinstance(node.confidence, (float, int)) or not math.isfinite(node.confidence) or not 0 <= node.confidence <= 1:
        raise EvaluationError("Confidence ngoài khoảng [0, 1]")
    index = bisect.bisect_right(cfg.bins, node.confidence)
    return [math.log(cfg.emission_false[index]), math.log(cfg.emission_true[index])]

def log_likelihood(node: BeliefNode, cfg: Config) -> List[float]:
    values = emission(node, cfg)
    if not node.children:
        return values
    child_values = [log_likelihood(child, cfg) for child in node.children]
    
    if node.method == "decompose":
        # Dynamic programming cho hội logic (AND decomposition) trong log-space
        all_true, any_false = 0.0, NEG_INF
        for false_val, true_val in child_values:
            any_false = logsumexp([any_false + logsumexp([false_val, true_val]), all_true + false_val])
            all_true += true_val
        count = len(child_values)
        log_non_all_true = count * math.log(2) + math.log1p(-math.exp(-count * math.log(2)))
        values[0] += any_false - log_non_all_true
        values[1] += all_true
    else:
        for relation, child in zip(node.relations, child_values):
            transition = TRANSITIONS[relation]
            for parent in (0, 1):
                trans_probs = [transition[parent][z] for z in (0, 1)]
                terms = []
                for z in (0, 1):
                    p = trans_probs[z]
                    if p > 0:
                        terms.append(math.log(p) + child[z])
                if terms:
                    values[parent] += logsumexp(terms)
    return values

def posterior_true(node: BeliefNode, cfg: Config) -> float:
    lf, lt = log_likelihood(node, cfg)
    lt += math.log(cfg.prior_true)
    lf += math.log1p(-cfg.prior_true)
    denom = logsumexp([lf, lt])
    if not math.isfinite(denom):
        raise EvaluationError("Không thể chuẩn hóa posterior")
    return math.exp(lt - denom)

def unique_texts(values: List[str]) -> List[str]:
    res, seen = [], set()
    for v in values:
        s = v.strip()
        if s and s not in seen:
            res.append(s)
            seen.add(s)
    return res

# ===== DỰNG CÂY NIỀM TIN (TREE BUILDER) =====
def build_tree(statement: str, backend: OpenAIBackend, cfg: Config,
               depth: int = 0, ancestors: Tuple[str, ...] = (), counter: Optional[List[int]] = None,
               context: Optional[str] = None) -> BeliefNode:
    counter = [0] if counter is None else counter
    counter[0] += 1
    if counter[0] > cfg.max_nodes:
        raise EvaluationError("Cây vượt max_nodes")

    judged = with_question_context(context, statement) if context else statement
    node = BeliefNode(statement=statement, confidence=backend.confidence(judged))
    if judged != statement:
        node.judged_statement = judged

    if depth >= cfg.depth:
        return node

    try:
        decomposition = backend.json("decompose", {"statement": statement})
    except InvalidDecompositionError as exc:
        node.notes.append(f"Bỏ qua phân rã lỗi: {exc}")
        decomposition = {"claims": [statement], "complete": True, "joint_equivalence": False}

    parts = unique_texts(decomposition.get("claims", []))
    if len(parts) > 1 and decomposition.get("joint_equivalence") and decomposition.get("complete"):
        if len(parts) <= cfg.max_decomposition and not any(x in ancestors + (statement,) for x in parts):
            check_decomposition_context(statement, parts)
            node.method = "decompose"
            node.children = [
                build_tree(p, backend, cfg, depth + 1, ancestors + (statement,), counter, context)
                for p in parts
            ]
            return node

    # Mở rộng logic hoặc bản sửa lỗi
    expanded = backend.json("expand", {"statement": statement, "branches": cfg.branches})
    candidates = unique_texts(expanded.get("children", []))[:cfg.branches]
    node.method = expanded.get("strategy", "logical")

    for child in candidates:
        if child in ancestors + (statement,):
            continue
        rel_pair = backend.json("relation", {"parent": statement, "child": child})
        relation = map_relation(rel_pair.get("forward", "neutral"), rel_pair.get("reverse", "neutral"))
        if relation == "neutral":
            continue
        child_depth = cfg.depth if node.method == "correction" else depth + 1
        child_node = build_tree(child, backend, cfg, child_depth, ancestors + (statement,), counter, context)
        node.children.append(child_node)
        node.relations.append(relation)

    if not node.children:
        node.method = "leaf"

    return node

# ===== TỔNG HỢP RỦI RO & CHẨN ĐOÁN LỖI =====
def aggregate_risk(checks: List[dict], cfg: Config) -> dict:
    scored = [x for x in checks if x["risk"] is not None]
    if not scored:
        return {"risk": 0.0, "raw_max_risk": 0.0, "capped": False, "corroborated": False}
    raw = max(x["risk"] for x in scored)
    primary = [x["risk"] for x in scored if x["category"] in ("answer_assignment", "explanation")]
    strong = [x for x in scored if x["risk"] >= CORROBORATION_RISK]
    corroborated = len(strong) >= 2 and any(x["category"] in ("answer_assignment", "explanation") for x in strong)
    capped = raw >= cfg.high_threshold and not corroborated
    return {
        "risk": (cfg.high_threshold - 0.1) if capped else raw,
        "raw_max_risk": raw,
        "capped": capped,
        "corroborated": corroborated,
        "primary_risk": max(primary, default=None)
    }

def consistency_statement(question_data: dict) -> str:
    return (
        "Kiểm tra tính nhất quán của dữ liệu câu hỏi trắc nghiệm dưới đây. "
        "Phát biểu cần đánh giá: Lời giải thích ủng hộ và chứng minh cho các lựa chọn được đánh dấu is_correct=true "
        "theo đúng yêu cầu của đề và không tự mâu thuẫn. Không bắt buộc phân tích mọi phương án nhiễu. "
        "DỮ LIỆU NGUYÊN VĂN: " + canonical(question_data)
    )

# ===== HÀM ĐÁNH GIÁ CHÍNH (MAIN AUDIT PIPELINE) =====


def max_tree_depth(node: BeliefNode) -> int:
    return max((1 + max_tree_depth(child) for child in node.children), default=0)


def true_false_expectation(content: str, options: List[OptionDto]) -> Optional[bool]:
    labels = {"đúng": True, "true": True, "sai": False, "false": False}
    values = [labels.get(unicodedata.normalize("NFC", opt.text).strip().casefold()) for opt in options]
    marked = [label for label, opt in zip(values, options) if opt.is_correct]
    if len(values) == 2 and set(values) == {True, False} and len(marked) == 1:
        if "?" not in content and not re.match(r"^(chọn|hãy chọn|which|is|does)\b", content, re.IGNORECASE):
            return marked[0]
    return None


def verified_claims(result: dict, field: str, source: str) -> List[str]:
    if not result["coverage_complete"]:
        raise EvaluationError("Phân tích chưa đầy đủ")
    claims = []
    for claim in result[field]:
        statement = claim["statement"].strip()
        quote = claim["source_quote"]
        if not statement or not quote.strip() or quote not in source:
            raise EvaluationError("Trích đoạn không khớp với dữ liệu gốc")
        claims.append(statement)
    return claims


def evaluate_question(req: QuestionAuditRequest, backend: OpenAIBackend, cfg: Config) -> BtpropAuditResponse:
    content = req.content.strip()
    options = req.options
    option_keys = {opt.key for opt in options}
    if not content or len(options) < 2 or not any(opt.is_correct for opt in options):
        raise ValueError("Cần nội dung câu hỏi, ít nhất hai lựa chọn và một đáp án đúng")
    if len(option_keys) != len(options) or any(not key.strip() for key in option_keys):
        raise ValueError("Khóa lựa chọn phải khác nhau và không được để trống")

    checks = []
    issues = []
    violations = []
    depth = 0
    incomplete = False
    truth = true_false_expectation(content, options)
    context = content if truth is None else None

    def check(check_id, category, statement, expected, *, direct=False):
        nonlocal depth, incomplete
        try:
            if direct:
                probability = backend.confidence(statement)
            else:
                tree = build_tree(statement, backend, cfg, context=context)
                probability = posterior_true(tree, cfg)
                depth = max(depth, max_tree_depth(tree))
            risk = 100 * (1 - probability if expected else probability)
            checks.append({"id": check_id, "category": category, "statement": statement,
                           "risk": risk, "posterior_true": probability})
        except EvaluationError as exc:
            incomplete = True
            issues.append(f"{check_id}: {exc}")

    blind = {"content": content, "options": [{"key": opt.key, "text": opt.text} for opt in options]}
    try:
        analysis = backend.json("analyze", blind)
        premises = verified_claims(analysis, "premises", content)
        claims_by_key = {item["key"]: item["statement"].strip() for item in analysis["options"]}
        if (len(analysis["options"]) != len(options) or set(claims_by_key) != option_keys
                or not all(claims_by_key.values())):
            raise EvaluationError("Phân tích thiếu hoặc trùng lựa chọn")
        ambiguities = list(dict.fromkeys(analysis["ambiguities"]))
        if ambiguities:
            reviews = backend.json("ambiguity", {"question": blind, "warnings": ambiguities})["reviews"]
            if len(reviews) != len(ambiguities) or {item["warning"] for item in reviews} != set(ambiguities):
                raise EvaluationError("Thẩm định thiếu cảnh báo ngữ cảnh")
            for item in reviews:
                if not item["material"]:
                    continue
                a, b = set(item["suitable_keys_a"]), set(item["suitable_keys_b"])
                if (not item["context_a"].strip() or not item["context_b"].strip()
                        or item["context_a"].strip() == item["context_b"].strip()
                        or a == b or (a | b) - option_keys):
                    raise EvaluationError("Chưa xác minh được cảnh báo ngữ cảnh")
                incomplete = True
                violations.append(ViolationDto(type="AMBIGUITY", node_statement=content,
                                                detail=item["reason"], severity="MEDIUM"))
        for opt in options:
            check(f"option:{opt.key}", "answer_assignment", claims_by_key[opt.key], opt.is_correct)
        if truth is not None:
            check("claim", "content", content, truth)
        else:
            for index, premise in enumerate(premises, 1):
                check(f"premise:{index}", "content", premise, True)
    except EvaluationError as exc:
        incomplete = True
        issues.append(f"Đề bài: {exc}")

    explanation = (req.explanation or "").strip()
    if not explanation:
        incomplete = True
        issues.append("Thiếu lời giải thích")
    else:
        try:
            claims = verified_claims(backend.json("explanation", {"content": content, "explanation": explanation}),
                                     "claims", explanation)
            for index, claim in enumerate(claims, 1):
                check(f"explanation:{index}", "explanation", claim, True)
            check("explanation:consistency", "explanation", consistency_statement({
                "content": content, "options": [opt.model_dump() for opt in options], "explanation": explanation,
            }), True, direct=True)
        except EvaluationError as exc:
            incomplete = True
            issues.append(f"Lời giải: {exc}")

    scores = {row["id"][7:]: row["posterior_true"] for row in checks if row["category"] == "answer_assignment"}
    suitable_keys = []
    if len(scores) == len(options) and not incomplete:
        suitable_keys = [opt.key for opt in options if scores[opt.key] >= 0.7]
        missed = [opt.key for opt in options if not opt.is_correct and opt.key in suitable_keys]
        wrong = [opt.key for opt in options if opt.is_correct and scores[opt.key] <= 0.3]
        if missed:
            violations.append(ViolationDto(type="MULTIPLE_ANSWERS", node_statement=content,
                                            detail=f"Có thể thiếu đáp án đúng: {', '.join(missed)}", severity="HIGH"))
        if wrong:
            violations.append(ViolationDto(type="WRONG_ANSWER", node_statement=content,
                                            detail=f"Có thể đánh dấu sai: {', '.join(wrong)}", severity="HIGH"))
        if all(score <= 0.3 for score in scores.values()):
            violations.append(ViolationDto(type="NO_ANSWER", node_statement=content,
                                            detail="Mọi lựa chọn đều có độ tin cậy thấp", severity="HIGH"))

    for row in checks:
        if row["risk"] >= cfg.low_threshold:
            issues.append(f"{row['id']}: rủi ro {row['risk']:.1f}/100 — {row['statement'][:100]}")
            if row["category"] in ("content", "explanation") and row["risk"] >= cfg.high_threshold:
                violations.append(ViolationDto(
                    type="PREMISE_ERROR" if row["category"] == "content" else "EXPLANATION_ERROR",
                    node_statement=row["statement"], detail=f"Rủi ro {row['risk']:.1f}/100", severity="HIGH",
                ))

    risk_score = None if incomplete else aggregate_risk(checks, cfg)["risk"]
    if risk_score is None:
        risk_label = "chưa kiểm chứng xong"
    elif risk_score < cfg.low_threshold:
        risk_label = "thấp"
    elif risk_score < cfg.high_threshold:
        risk_label = "trung bình"
    else:
        risk_label = "cao"
    return BtpropAuditResponse(
        is_hallucinated=risk_score is not None and risk_score >= cfg.high_threshold,
        confidence_score=None if risk_score is None else round(1 - risk_score / 100, 3),
        risk_score=None if risk_score is None else round(risk_score, 1),
        risk_label=risk_label,
        belief_tree_depth=depth,
        violations=violations,
        issues=issues,
        likely_suitable_options=suitable_keys,
        status="incomplete" if incomplete else "complete",
        mode=backend.mode,
    )
