package com.frozenheart.backend.modules.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RateQuestionRequest(
        @NotNull(message = "Mức đánh giá không được để trống")
        @Min(value = 0, message = "Đánh giá tối thiểu là 0")
        @Max(value = 4, message = "Đánh giá tối đa là 4")
        Integer rating,

        Boolean isError,

        String comment
    ) {
}
