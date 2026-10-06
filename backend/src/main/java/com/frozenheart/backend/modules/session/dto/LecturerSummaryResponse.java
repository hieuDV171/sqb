package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Thông tin tóm tắt của giảng viên")
public record LecturerSummaryResponse(
        @Schema(description = "ID tài khoản giảng viên", example = "2")
        Long id,

        @Schema(description = "Họ và tên giảng viên", example = "TS. Nguyễn Văn B")
        String fullName,

        @Schema(description = "Email trường (@hust.edu.vn)", example = "b.nv@hust.edu.vn")
        String email,

        @Schema(description = "Mã cán bộ giảng viên", example = "#001.002.00034")
        String studentLecturerCode,

        @Schema(description = "Khoa/Viện công tác", example = "Trường Công nghệ Thông tin và Truyền thông")
        String schoolFaculty
) {}
