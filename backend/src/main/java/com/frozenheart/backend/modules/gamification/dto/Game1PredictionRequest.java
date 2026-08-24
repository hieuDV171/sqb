package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record Game1PredictionRequest(
        @NotNull(message = "số dự đoán không được bỏ trống")
        @Min(value = 0, message = "số dự đoán không hợp lệ")
        Integer predictedCount
) {}
