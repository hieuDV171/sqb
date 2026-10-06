package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu dự đoán Game 2: Số câu hỏi LLM và Con người được phê duyệt vào ngân hàng đề")
public record Game2PredictionRequest(
        @Schema(description = "ID phiên đề xuất", example = "12")
        @NotNull(message = "session_id (mã phiên) không được bỏ trống")
        Long sessionId,

        @Schema(description = "Số câu hỏi do LLM tạo được duyệt (APPROVED)", example = "3")
        Integer predictedLlmCount,

        @Schema(description = "Số câu hỏi do con người tạo được duyệt (APPROVED)", example = "5")
        Integer predictedHumanCount
) {}

