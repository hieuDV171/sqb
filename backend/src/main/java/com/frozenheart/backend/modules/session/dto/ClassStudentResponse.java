package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

@Builder
@Schema(description = "Thông tin chi tiết của sinh viên trong lớp học phần")
public record ClassStudentResponse(
        @Schema(description = "ID người dùng", example = "25")
        Long userId,

        @Schema(description = "Mã số sinh viên (MSSV)", example = "20224980")
        String studentCode,
                
        @Schema(description = "Họ và tên sinh viên", example = "Đồng Văn Hiếu")
        String fullName,

        @Schema(description = "Email trường (@sis.hust.edu.vn)", example = "hieu.dv224980@sis.hust.edu.vn")
        String email,

        @Schema(description = "Lớp sinh hoạt", example = "Khoa học máy tính 01-K68")
        String className,

        @Schema(description = "Khoa/Viện đào tạo", example = "Trường Công nghệ Thông tin và Truyền thông")
        String schoolFaculty,

        @Schema(description = "Chuyên ngành", example = "Khoa học máy tính")
        String major,

        @Schema(description = "Ngày sinh", example = "2005-10-15")
        LocalDate dateOfBirth,

        @Schema(description = "Thời điểm ghi danh vào lớp học phần")
        Instant enrolledAt
) {}
