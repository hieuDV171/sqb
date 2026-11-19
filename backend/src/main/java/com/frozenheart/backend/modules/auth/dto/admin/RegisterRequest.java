package com.frozenheart.backend.modules.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "ADMIN tạo một tài khoản và gửi OTP email; tài khoản còn phải hoàn tất onboarding")
public record RegisterRequest(
        @Schema(description = "Email tài khoản được ADMIN cấp hoặc quản lý", example = "student@example.edu")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        @Email(message = "INVALID_PARAMETER_VALUE")
        String email,
        @Schema(description = "Mật khẩu cho tài khoản; không ghi vào log hoặc lưu dạng rõ trên client", example = "Example!Password123")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        @Size(min = 8, message = "INVALID_PARAMETER_VALUE")
        String password,
        @Schema(description = "Định danh ổn định của thiết bị/cài đặt; không dùng chung UUID mẫu giữa nhiều thiết bị", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        String deviceId,
        @Schema(description = "Vai trò tài khoản quyết định quyền truy cập nghiệp vụ")
        @NotNull(message = "MISSING_REQUIRED_PARAMETER")
        UserRoleDto role,
        @Schema(description = "Múi giờ IANA dùng để hiển thị và tính hoạt động theo ngày", example = "Asia/Ho_Chi_Minh")
        String timezone
) {

}
