package com.frozenheart.backend.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mật khẩu tạm bàn giao một lần cho ADMIN sau khi tạo/reset thành công; không lưu dạng rõ")
public record IssuedAccountCredential(
        @Schema(description = "ID tài khoản được cấp mật khẩu tạm", example = "7")
        Long userId,
        @Schema(description = "Email đăng nhập của tài khoản", example = "an.nv20211234@sis.hust.edu.vn")
        String email,
        @Schema(description = "Họ và tên người nhận tài khoản", example = "Nguyễn Văn An")
        String fullName,
        @Schema(description = "Mã số sinh viên hoặc giảng viên", example = "20211234")
        String studentLecturerCode,
        @Schema(description = "Mật khẩu tạm chỉ bàn giao một lần cho ADMIN sau khi cấp thành công", example = "Q7!vR2@kT9#pL4&x")
        String temporaryPassword) {
    @Override
    public String toString() {
        return "IssuedAccountCredential[userId=" + userId + ", temporaryPassword=[REDACTED]]";
    }
}
