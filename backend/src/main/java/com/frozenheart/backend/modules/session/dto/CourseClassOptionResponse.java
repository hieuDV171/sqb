package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Lớp học phần có thể hiển thị trong danh sách đề xuất câu hỏi")
public record CourseClassOptionResponse(
        @Schema(description = "ID lớp học phần", example = "42")
        Long courseClassId,
        @Schema(description = "Mã lớp học phần", example = "161234")
        String classCode,
        @Schema(description = "ID môn học", example = "42")
        Long subjectId,
        @Schema(description = "Mã môn học", example = "IT3290")
        String subjectCode,
        @Schema(description = "Tên môn học", example = "Cơ sở dữ liệu")
        String subjectName,
        @Schema(description = "ID học kỳ", example = "42")
        Long semesterId,
        @Schema(description = "Tên học kỳ", example = "2026.1")
        String semesterName,
        @Schema(description = "Lớp thuộc học kỳ đang hoạt động hay không", example = "true")
        boolean activeSemester,
        @Schema(description = "Người gọi được phép đề xuất bộ câu hỏi vào lớp này", example = "true")
        boolean canPropose
) {
}
