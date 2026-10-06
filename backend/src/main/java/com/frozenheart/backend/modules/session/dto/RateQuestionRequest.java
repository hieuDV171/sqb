package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Schema(description = "Yêu cầu đánh giá chất lượng câu hỏi")
@Builder
public record RateQuestionRequest(
        @Schema(description = "Mức điểm đánh giá [0; 4] (0: sai hoàn toàn, 1: LLM bịa, 2: người hỏi chưa vững kiến thức, 3: kiến thức cơ bản, 4: câu hỏi hay)", example = "4")
        @NotNull(message = "Mức đánh giá không được để trống")
        @Min(value = 0, message = "Đánh giá tối thiểu là 0")
        @Max(value = 4, message = "Đánh giá tối đa là 4")
        Integer rating,

        @Schema(description = "Đánh dấu câu hỏi bị sai đề hoặc lỗi kiến thức (nếu true, điểm đánh giá tự động là 0)", example = "false")
        Boolean isError,

        @Schema(description = "Bình luận hoặc góp ý chi tiết cho câu hỏi", example = "Câu hỏi phân loại tốt, giải thích rõ ràng và có tính ứng dụng cao.")
        String comment
    ) {
}

