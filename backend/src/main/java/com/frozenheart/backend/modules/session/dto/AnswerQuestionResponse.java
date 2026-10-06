package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả chấm câu trả lời")
@Builder
public record AnswerQuestionResponse(
        @Schema(description = "Kết quả trả lời đúng hay sai", example = "true")
        boolean isCorrect,

        @Schema(description = "Khóa đáp án đúng", example = "A")
        String correctAnswer,

        @Schema(description = "Lời giải chi tiết câu hỏi", example = "Thuật toán Round Robin cấp phát quantum time đồng đều...")
        String explanation
) {}

