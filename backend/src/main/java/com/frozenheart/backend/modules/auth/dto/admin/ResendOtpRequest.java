package com.frozenheart.backend.modules.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Email tài khoản cần cấp lại OTP kích hoạt trong luồng quản trị")
public record ResendOtpRequest(
        @Schema(description = "Email tài khoản được ADMIN cấp hoặc quản lý", example = "student@example.edu")
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    @Email(message = "INVALID_PARAMETER_VALUE")
    String email
) {
}
