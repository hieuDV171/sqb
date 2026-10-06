package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Kết quả import lớp học phần và ghi danh sinh viên từ file Excel")
public record ExcelImportClassResult(
        @Schema(description = "ID lớp học phần", example = "100")
        Long courseClassId,

        @Schema(description = "Mã lớp học phần bóc tách từ Excel", example = "171146")
        String classCode,

        @Schema(description = "Mã môn học bóc tách từ Excel", example = "IT3180")
        String subjectCode,

        @Schema(description = "Tên môn học tương ứng trong hệ thống", example = "Nhập môn Công nghệ phần mềm")
        String subjectName,

        @Schema(description = "Tên học kỳ đang hoạt động", example = "20241")
        String semesterName,

        @Schema(description = "Tổng số dòng sinh viên đọc được từ file Excel", example = "60")
        int totalRowsInFile,

        @Schema(description = "Số lượng tài khoản sinh viên mới được tạo tự động", example = "55")
        int newUsersCreated,

        @Schema(description = "Số lượng sinh viên đã tồn tại tài khoản từ trước", example = "5")
        int existingUsersFound,
        
        @Schema(description = "Số lượng sinh viên mới được ghi danh vào lớp học phần", example = "58")
        int newEnrollments,

        @Schema(description = "Số lượng sinh viên đã ghi danh vào lớp này từ trước", example = "2")
        int alreadyEnrolledCount
    ) {
}
