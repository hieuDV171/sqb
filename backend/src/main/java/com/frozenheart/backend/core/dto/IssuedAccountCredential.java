package com.frozenheart.backend.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mật khẩu tạm bàn giao một lần cho ADMIN sau khi tạo/reset thành công; không lưu dạng rõ")
public record IssuedAccountCredential(Long userId, String email, String fullName,
                                      String studentLecturerCode, String temporaryPassword) {
    @Override
    public String toString() {
        return "IssuedAccountCredential[userId=" + userId + ", temporaryPassword=[REDACTED]]";
    }
}
