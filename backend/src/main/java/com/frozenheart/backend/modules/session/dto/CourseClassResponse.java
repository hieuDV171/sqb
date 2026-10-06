package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;

@Builder
@Schema(description = "Thông tin chi tiết của một lớp học phần")
public record CourseClassResponse(
        @Schema(description = "ID lớp học phần", example = "100")
        Long courseClassId,

        @Schema(description = "Mã lớp học phần", example = "171146")
        String classCode,

        @Schema(description = "ID môn học", example = "10")
        Long subjectId,

        @Schema(description = "Mã môn học", example = "IT3180")
        String subjectCode,

        @Schema(description = "Tên môn học", example = "Nhập môn Công nghệ phần mềm")
        String subjectName,

        @Schema(description = "ID học kỳ", example = "1")
        Long semesterId,

        @Schema(description = "Tên học kỳ", example = "20241")
        String semesterName,

        @Schema(description = "ID giảng viên phụ trách", example = "2")
        Long lecturerId,

        @Schema(description = "Họ và tên giảng viên phụ trách", example = "TS. Nguyễn Văn B")
        String lecturerName,

        @Schema(description = "Tổng số sinh viên đã ghi danh vào lớp", example = "65")
        int totalStudents,

        @Schema(description = "Thời điểm khởi tạo lớp học phần")
        Instant createdAt
    ) {
}
