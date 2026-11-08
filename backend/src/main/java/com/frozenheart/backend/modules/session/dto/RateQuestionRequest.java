package com.frozenheart.backend.modules.session.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "Yêu cầu đánh giá chất lượng câu hỏi")
@Builder
public record RateQuestionRequest(
        @Schema(description = "Mức điểm từ 0 đến 4, tương ứng 1 đến 5 sao", example = "4")
        @NotNull(message = "Mức đánh giá không được để trống")
        @Min(value = 0, message = "Đánh giá tối thiểu là 0")
        @Max(value = 4, message = "Đánh giá tối đa là 4")
        Integer rating,

        @Schema(description = "Bình luận hoặc góp ý chi tiết cho câu hỏi", example = "Câu hỏi phân loại tốt, giải thích rõ ràng và có tính ứng dụng cao.")
        @Size(max = 500) String comment
    ) {
    @JsonAnySetter
    private void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("Trường không hợp lệ trong đánh giá sao: " + field);
    }
}

