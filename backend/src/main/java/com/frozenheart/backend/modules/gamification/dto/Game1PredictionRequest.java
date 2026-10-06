package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu dự đoán Game 1: Số lượng người tham gia nộp câu hỏi ngày mai của lớp học phần")
public record Game1PredictionRequest(
        @Schema(description = "Số lượng sinh viên dự đoán sẽ đề xuất câu hỏi vào ngày mai", example = "5")
        @NotNull(message = "Số dự đoán không được bỏ trống")
        @Min(value = 1, message = "Số lượng sinh viên dự đoán tối thiểu là 1")
        Integer predictedCount,

        @Schema(description = "ID lớp học phần", example = "101")
        @NotNull(message = "Lớp học phần không được bỏ trống")
        Long courseClassId
) {}

