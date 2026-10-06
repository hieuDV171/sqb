package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Kết quả sau khi đánh giá câu hỏi")
@Builder
public record RateQuestionResponse(
        @Schema(description = "Điểm đánh giá trung bình mới của câu hỏi", example = "3.75")
        double newAvgRating,

        @Schema(description = "Tổng số lượt đánh giá mới", example = "12")
        int newRatingCount
) {}

