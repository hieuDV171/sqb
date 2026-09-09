package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record Game1PredictionRequest(
        @NotNull(message = "Số dự đoán không được bỏ trống")
        @Min(value = 1, message = "Số lượng sinh viên dự đoán tối thiểu là 1")
        Integer predictedCount,

        @NotNull(message = "Lớp học phần không được bỏ trống")
        Long courseClassId
) {}
