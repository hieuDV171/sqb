package com.frozenheart.backend.modules.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Yêu cầu xác thực email của tài khoản đang đăng nhập")
public record ConfirmEmailVerificationRequest(
        @Schema(description = "Mã OTP 6 chữ số đã gửi tới email của người dùng", example = "123456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
        @Pattern(regexp = "\\d{6}", message = "INVALID_PARAMETER_VALUE")
        String verifyCode
) {
}
