package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record Game4PredictionRequest(
        @NotNull(message = "không được bỏ trống id môn học (subject_id)")
        Long subjectId,

        @NotNull(message = "không được bỏ trống số dự đoán")
        @Min(value = 0, message = "số dự đoán không hợp lệ")
        Integer predictedBankSize
) {}
