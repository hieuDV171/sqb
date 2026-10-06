package com.frozenheart.backend.modules.auth.dto.user;

import com.frozenheart.backend.core.entity.user.DevicePlatform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin yêu cầu đăng nhập hệ thống")
public record LoginRequest(
    @Schema(description = "Địa chỉ email sinh viên hoặc giảng viên", example = "hieu.dv224980@sis.hust.edu.vn", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    @Email(message = "INVALID_PARAMETER_VALUE")
    String email,

    @Schema(description = "Mật khẩu tài khoản", example = "20224980", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String password,

    @Schema(description = "Định danh thiết bị duy nhất (UUID hoặc Device Fingerprint)", example = "web-browser-550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String deviceId,
    
    @Schema(description = "Firebase Cloud Messaging Token dùng để đẩy thông báo push", example = "fcm_token_example_xyz")
    String fcmToken,
                    
    @Schema(description = "Nền tảng thiết bị đang đăng nhập", example = "WEB", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "MISSING_REQUIRED_PARAMETER")
    DevicePlatform platform,

    @Schema(description = "Tên hiển thị của thiết bị", example = "Chrome on Windows 11")
    String deviceName,

    @Schema(description = "Phiên bản hệ điều hành", example = "Windows 11 Build 22631")
    String osVersion,

    @Schema(description = "Phiên bản ứng dụng client", example = "1.0.0")
    String appVersion

) {
}
