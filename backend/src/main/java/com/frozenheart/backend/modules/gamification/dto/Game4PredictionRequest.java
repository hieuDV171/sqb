package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu dự đoán Game 4: Tổng số câu hỏi ngân hàng đề môn học khi kết thúc kỳ")
public record Game4PredictionRequest(
        @Schema(description = "ID môn học", example = "10")
        @NotNull(message = "không được bỏ trống id môn học (subject_id)")
        Long subjectId,

        @Schema(description = "Dự đoán tổng số lượng câu hỏi được duyệt trong ngân hàng đề", example = "250")
        @NotNull(message = "không được bỏ trống số dự đoán")
        @Min(value = 0, message = "số dự đoán không hợp lệ")
        Integer predictedBankSize
) {}

