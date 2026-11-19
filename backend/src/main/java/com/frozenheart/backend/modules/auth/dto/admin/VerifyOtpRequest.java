package com.frozenheart.backend.modules.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "ADMIN xác nhận OTP email; chỉ tài khoản ACTIVE mới nhận phiên đầy đủ")
public record VerifyOtpRequest(
        @Schema(description = "Email tài khoản được ADMIN cấp hoặc quản lý", example = "student@example.edu")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        @Email(message = "INVALID_PARAMETER_VALUE")
        String email,
        @Schema(description = "Mã OTP 6 chữ số được gửi đến email tài khoản", example = "123456")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        String verifyCode,
        @Schema(description = "Định danh ổn định của thiết bị/cài đặt; không dùng chung UUID mẫu giữa nhiều thiết bị", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        String deviceId
) {

}
