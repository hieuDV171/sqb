package com.frozenheart.backend.modules.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.user.UserRole;

@Schema(description = "Tài khoản tìm thấy để quản trị viên trao hoặc thu hồi huy hiệu")
public record BadgeAccountResponse(
        @Schema(description = "ID tài khoản trong SQB", example = "42")
        Long userId,
        @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
        String fullName,
        @Schema(description = "Email tài khoản được ADMIN cấp hoặc quản lý", example = "student@example.edu")
        String email,
        @Schema(description = "Vai trò tài khoản quyết định quyền truy cập nghiệp vụ")
        UserRole role) {
}
