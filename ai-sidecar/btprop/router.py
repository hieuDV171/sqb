import os
from fastapi import APIRouter, HTTPException, status
import logging

from .config import Config
from .schemas import QuestionAuditRequest, BtpropAuditResponse
from .backend import OpenAIBackend
from .engine import evaluate_question

logger = logging.getLogger("btprop_router")
router = APIRouter()

@router.post("/audit", response_model=BtpropAuditResponse, summary="Kiểm chứng câu hỏi bằng Cây Niềm Tin (BTProp)")
async def audit_question(req: QuestionAuditRequest):
    """
    Endpoint kiểm tra ảo giác và sai sót kiến thức của câu hỏi trắc nghiệm.
    - Hỗ trợ đa môn học (subject, topic)
    - Hỗ trợ câu hỏi nhiều hơn 4 lựa chọn và nhiều đáp án đúng
    - Chạy thuật toán cây niềm tin Belief Tree Propagation (BTProp)
    """
    try:
        api_key = os.getenv("OPENAI_API_KEY", "")
        if not api_key:
            logger.warning("[BTProp] Chưa cấu hình OPENAI_API_KEY. Trả về fallback unverified.")
            return BtpropAuditResponse(
                is_hallucinated=False,
                confidence_score=0.5,
                risk_score=50.0,
                risk_label="chưa cấu hình API Key",
                belief_tree_depth=0,
                violations=[],
                issues=["Chưa cấu hình OPENAI_API_KEY trên AI Sidecar"],
                likely_suitable_options=[],
                status="unverified",
                mode="no_api_key"
            )

        model = os.getenv("BTPROP_MODEL", "gpt-4.1-2025-04-14")
        depth = 1 if req.fast_mode else int(os.getenv("BTPROP_DEPTH", "2"))
        cfg = Config(model=model, depth=depth)
        
        backend = OpenAIBackend(api_key=api_key, cfg=cfg, subject=req.subject, topic=req.topic)
        result = evaluate_question(req, backend, cfg)
        return result

    except ValueError as ve:
        logger.warning(f"[BTProp] Invalid input: {ve}")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(ve))
    except Exception as e:
        logger.error(f"[BTProp] Internal error during audit: {e}", exc_info=True)
        # Thay vì trả về 500 làm sập luồng, trả về audit status error
        return BtpropAuditResponse(
            is_hallucinated=False,
            confidence_score=0.5,
            risk_score=50.0,
            risk_label="lỗi kiểm thử",
            belief_tree_depth=0,
            violations=[],
            issues=[f"Lỗi hệ thống kiểm chứng: {str(e)}"],
            likely_suitable_options=[],
            status="error",
            mode="error"
        )
