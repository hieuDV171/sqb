package com.frozenheart.backend.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả áp dụng/loại bỏ chỉnh sửa AI")
@Builder
public record AiApplyResponse(
        @Schema(description = "ID câu hỏi", example = "42")
        Long questionId,

        @Schema(description = "Trạng thái sau khi áp dụng (APPLIED hoặc DISCARDED)", example = "APPLIED")
        String status,

        @Schema(description = "Điểm tin cậy của mô hình", example = "0.98")
        double confidenceScore,

        @Schema(description = "Có bị đánh dấu ảo giác hay không", example = "false")
        boolean hallucination
) {}
