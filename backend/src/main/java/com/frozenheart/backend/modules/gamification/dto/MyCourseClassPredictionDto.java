package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Thông tin lớp học phần để dự đoán Game 1")
@Builder
public record MyCourseClassPredictionDto(
        @Schema(description = "ID lớp học phần", example = "101")
        Long courseClassId,

        @Schema(description = "Mã lớp học phần", example = "IT3080_01")
        String classCode,

        @Schema(description = "Tên học kỳ", example = "2024.1")
        String semester,

        @Schema(description = "ID môn học", example = "10")
        Long subjectId,

        @Schema(description = "Tên môn học", example = "Hệ điều hành")
        String subjectName,

        @Schema(description = "Họ tên giảng viên phụ trách", example = "TS. Nguyễn Văn A")
        String lecturerName,

        @Schema(description = "Người dùng đã đặt cược cho ngày mai ở lớp này chưa", example = "false")
        boolean alreadyPredicted,

        @Schema(description = "Số lượng sinh viên đã dự đoán (nếu đã đặt)", example = "4")
        Integer predictedCount
) {}

