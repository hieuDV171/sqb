from typing import List, Optional, Any
from pydantic import BaseModel, Field

# ===== REQUEST DTO =====
class OptionDto(BaseModel):
    key: str
    text: str = Field(description="Nội dung phương án lựa chọn")
    is_correct: bool = Field(description="Phương án này có đúng hay không")
    media_url: Optional[str] = None
    media_id: Optional[int] = None

class QuestionAuditRequest(BaseModel):
    subject: Optional[str] = Field(default="Khoa học máy tính", description="Tên môn học (Toán, CSDL, Mạng, Web...)")
    topic: Optional[str] = Field(default=None, description="Chủ đề cụ thể của câu hỏi")
    content: str = Field(description="Nội dung câu hỏi")
    options: List[OptionDto] = Field(description="Danh sách các phương án trả lời")
    explanation: Optional[str] = Field(default="", description="Lời giải thích của câu hỏi")
    fast_mode: Optional[bool] = Field(default=False, description="Chế độ nhanh depth=1 cho phản hồi tức thì")

# ===== RESPONSE DTO =====
class ViolationDto(BaseModel):
    type: str = Field(description="Mã vi phạm: WRONG_ANSWER, MULTIPLE_ANSWERS, AMBIGUITY, EXPLANATION_ERROR, PREMISE_ERROR")
    node_statement: str = Field(description="Mệnh đề hoặc vị trí gây ra cảnh báo")
    detail: str = Field(description="Mô tả chi tiết nguyên nhân")
    severity: str = Field(description="Mức độ nghiêm trọng: HIGH, MEDIUM, LOW")

class BtpropAuditResponse(BaseModel):
    is_hallucinated: bool = Field(description="True nếu phát hiện rủi ro cao >= 70")
    confidence_score: float = Field(description="Độ tin cậy tổng thể (0.0 - 1.0)")
    risk_score: float = Field(description="Điểm rủi ro (0 - 100)")
    risk_label: str = Field(description="'thấp', 'trung bình', 'cao', 'lỗi'")
    belief_tree_depth: int = Field(description="Độ sâu cây niềm tin đã duyệt")
    violations: List[ViolationDto] = Field(default_factory=list, description="Danh sách các vi phạm/nghi vấn")
    issues: List[str] = Field(default_factory=list, description="Danh sách các cảnh báo dạng text")
    likely_suitable_options: List[str] = Field(default_factory=list, description="Các option keys mà cây niềm tin đánh giá là đúng")
    status: str = Field(default="complete", description="complete, incomplete, budget_stopped, api_blocked")
    mode: str = Field(default="live_openai")

# ===== OPENAI STRUCTURED OUTPUT SCHEMAS (JSON SCHEMAS) =====
def object_schema(properties):
    return {"type": "object", "properties": properties,
            "required": list(properties), "additionalProperties": False}

TEXT = {"type": "string"}
BOOL = {"type": "boolean"}
def array_schema(item):
    return {"type": "array", "items": item}

def enum_schema(*values):
    return {"type": "string", "enum": list(values)}

CLAIM_SCHEMA = object_schema({"statement": TEXT, "source_quote": TEXT})

OPENAI_SCHEMAS = {
    "ambiguity": object_schema({
        "reviews": array_schema(object_schema({
            "warning": TEXT,
            "material": BOOL,
            "context_a": TEXT,
            "context_b": TEXT,
            "suitable_keys_a": array_schema(TEXT),
            "suitable_keys_b": array_schema(TEXT),
            "reason": TEXT
        }))
    }),
    "analyze": object_schema({
        "premises": array_schema(CLAIM_SCHEMA),
        "options": array_schema(object_schema({"key": TEXT, "statement": TEXT})),
        "ambiguities": array_schema(TEXT),
        "coverage_complete": BOOL
    }),
    "explanation": object_schema({
        "claims": array_schema(CLAIM_SCHEMA),
        "coverage_complete": BOOL
    }),
    "decompose": object_schema({
        "claims": array_schema(TEXT),
        "complete": BOOL,
        "joint_equivalence": BOOL
    }),
    "expand": object_schema({
        "strategy": enum_schema("logical", "correction"),
        "children": array_schema(TEXT)
    }),
    "relation": object_schema({
        "forward": enum_schema("entailment", "neutral", "contradiction"),
        "reverse": enum_schema("entailment", "neutral", "contradiction")
    }),
    "diagnose": object_schema({
        "issues": array_schema(object_schema({
            "kind": enum_schema("wrong_answer", "multiple_answers", "no_answer", "explanation_error",
                                "premise_error", "ambiguity", "other"),
            "check_ids": array_schema(TEXT),
            "reason": TEXT
        }))
    }),
}
