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
        "Phát biểu cần đánh giá: Lời giải thích ủng hộ và chứng minh cho TẤT CẢ các lựa chọn được đánh dấu is_correct=true, "
        "đồng thời phân biệt/chỉ rõ lý do không chọn các lựa chọn is_correct=false theo đúng yêu cầu của đề bài. "
        "DỮ LIỆU NGUYÊN VĂN: " + canonical(question_data)
    )

# ===== HÀM ĐÁNH GIÁ CHÍNH (MAIN AUDIT PIPELINE) =====
def evaluate_question(req: QuestionAuditRequest, backend: OpenAIBackend, cfg: Config) -> BtpropAuditResponse:
    # 1. Chuẩn hóa & Validate câu hỏi (Hỗ trợ >= 2 options, >= 1 đáp án đúng)
    content = req.content.strip()
    if not content:
        raise ValueError("Nội dung câu hỏi không được rỗng")
    if len(req.options) < 2:
        raise ValueError("Câu hỏi phải có ít nhất 2 lựa chọn")
    
    correct_count = sum(1 for opt in req.options if opt.is_correct)
    if correct_count < 1:
        raise ValueError("Câu hỏi phải có ít nhất 1 phương án được đánh dấu đúng")

    checks = []
    issues = []
    violations = []

    # 2. Phân tích khách quan (Blind Analysis - Giấu nhãn đáp án và explanation)
    blind_input = {
        "content": content,
        "options": [{"key": opt.key, "text": opt.text} for opt in req.options]
    }
    analysis = backend.json("analyze", blind_input)
    by_key = {x["key"]: x["statement"] for x in analysis.get("options", [])}

    # Thẩm định mơ hồ
    raw_ambiguities = analysis.get("ambiguities", [])
    if raw_ambiguities:
        amb_review = backend.json("ambiguity", {"question": blind_input, "warnings": raw_ambiguities})
        for rev in amb_review.get("reviews", []):
            if rev.get("material"):
                issues.append(f"Mơ hồ/thiếu ngữ cảnh: {rev.get('reason')}")
                violations.append(ViolationDto(
                    type="AMBIGUITY",
                    node_statement="Đề bài thiếu điều kiện",
                    detail=rev.get("reason", "Thiếu điều kiện làm đổi đáp án"),
                    severity="MEDIUM"
                ))

    # 3. Tạo các bài kiểm tra cho từng Lựa chọn (Option Answer Assignment)
    for opt in req.options:
        stmt = by_key.get(opt.key, f"Lựa chọn {opt.key}: {opt.text} đáp ứng yêu cầu của câu hỏi.")
        check_row = {
            "id": f"option:{opt.key}",
            "category": "answer_assignment",
            "statement": stmt,
            "expected_true": opt.is_correct,
            "option_key": opt.key,
            "posterior_true": None,
            "risk": None,
            "error": None
        }
        try:
            tree = build_tree(stmt, backend, cfg, depth=0 if req.fast_mode else cfg.depth, context=content)
            prob = posterior_true(tree, cfg)
            risk = 100.0 * (1.0 - prob if opt.is_correct else prob)
            check_row["posterior_true"] = prob
            check_row["risk"] = risk
        except Exception as e:
            check_row["error"] = str(e)
            issues.append(f"Lỗi kiểm tra option {opt.key}: {e}")
        checks.append(check_row)

    # 4. Kiểm tra Lời giải thích (Explanation)
    explanation = (req.explanation or "").strip()
    if explanation:
        try:
            expl_data = backend.json("explanation", {"content": content, "explanation": explanation})
            for idx, claim in enumerate(expl_data.get("claims", []), 1):
                c_stmt = claim.get("statement", "")
                c_row = {
                    "id": f"explanation:{idx}",
                    "category": "explanation",
                    "statement": c_stmt,
                    "expected_true": True,
                    "posterior_true": None,
                    "risk": None,
                    "error": None
                }
                try:
                    c_tree = build_tree(c_stmt, backend, cfg, depth=0 if req.fast_mode else cfg.depth, context=content)
                    c_prob = posterior_true(c_tree, cfg)
                    c_row["posterior_true"] = c_prob
                    c_row["risk"] = 100.0 * (1.0 - c_prob)
                except Exception as e:
                    c_row["error"] = str(e)
                checks.append(c_row)

            # Meta check tính nhất quán của lời giải
            const_stmt = consistency_statement({"content": content, "options": [opt.model_dump() for opt in req.options], "explanation": explanation})
            const_conf = backend.confidence(const_stmt)
            checks.append({
                "id": "explanation:consistency",
                "category": "explanation",
                "statement": const_stmt,
                "expected_true": True,
                "posterior_true": const_conf,
                "risk": 100.0 * (1.0 - const_conf),
                "error": None
            })
        except Exception as e:
            issues.append(f"Lỗi phân tích lời giải: {e}")
    else:
        issues.append("Câu hỏi chưa có lời giải thích")

    # 5. So sánh tập hợp để phát hiện Thừa / Thiếu / Sai đáp án đúng
    option_checks = [c for c in checks if c["category"] == "answer_assignment" and c["posterior_true"] is not None]
    expected_correct = {opt.key for opt in req.options if opt.is_correct}
    suitable_keys = [c["option_key"] for c in option_checks if c["posterior_true"] >= 0.7]

    extra_keys = set(suitable_keys) - expected_correct
    if extra_keys:
        msg = f"Nghi ngờ thiếu đáp án đúng: các lựa chọn {sorted(list(extra_keys))} cũng có khả năng đúng nhưng chưa được tích chọn."
        issues.append(msg)
        violations.append(ViolationDto(
            type="MULTIPLE_ANSWERS",
            node_statement="Thiếu sót đáp án đúng",
            detail=msg,
            severity="HIGH"
        ))

    wrong_marked = expected_correct - set(suitable_keys)
    # Lọc ra những option có posterior thấp thực sự (< 0.4)
    really_wrong = [k for k in wrong_marked if any(c["option_key"] == k and c["posterior_true"] <= 0.4 for c in option_checks)]
    if really_wrong:
        msg = f"Nghi ngờ đánh dấu sai: các lựa chọn {really_wrong} được đánh dấu đúng nhưng cây niềm tin đánh giá rủi ro sai cao."
        issues.append(msg)
        violations.append(ViolationDto(
            type="WRONG_ANSWER",
            node_statement="Đánh dấu đáp án sai",
            detail=msg,
            severity="HIGH"
        ))

    if not suitable_keys and option_checks:
        msg = "Nghi ngờ không có lựa chọn nào đúng hoặc đề bài sai hoàn toàn."
        issues.append(msg)
        violations.append(ViolationDto(
            type="NO_ANSWER",
            node_statement="Không có đáp án phù hợp",
            detail=msg,
            severity="HIGH"
        ))

    # 6. Tính tổng hợp điểm rủi ro toàn câu
    agg = aggregate_risk(checks, cfg)
    risk_score = agg["risk"]
    is_hallucinated = risk_score >= cfg.high_threshold
    risk_label = "thấp" if risk_score < cfg.low_threshold else ("trung bình" if risk_score < cfg.high_threshold else "cao")

    # Ghi nhận các checks có risk cao vào violations nếu chưa có
    for c in checks:
        if c.get("risk") and c["risk"] >= cfg.low_threshold:
            issues.append(f"{c['id']}: rủi ro {c['risk']:.1f}/100 — {c['statement'][:100]}...")

    confidence_score = max(0.0, min(1.0, (100.0 - risk_score) / 100.0))

    return BtpropAuditResponse(
        is_hallucinated=is_hallucinated,
        confidence_score=round(confidence_score, 3),
        risk_score=round(risk_score, 1),
        risk_label=risk_label,
        belief_tree_depth=0 if req.fast_mode else cfg.depth,
        violations=violations,
        issues=issues,
        likely_suitable_options=suitable_keys,
        status="complete",
        mode=backend.mode
    )
