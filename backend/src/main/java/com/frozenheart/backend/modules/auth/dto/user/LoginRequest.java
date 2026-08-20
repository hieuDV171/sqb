package com.frozenheart.backend.modules.auth.dto.user;

import com.frozenheart.backend.core.entity.user.DevicePlatform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    @Email(message = "INVALID_PARAMETER_VALUE")
    String email,

    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    @Size(min = 8, message = "INVALID_PARAMETER_VALUE")
    String password,

    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String deviceId,
    
    String fcmToken,
                    
    @NotNull(message = "MISSING_REQUIRED_PARAMETER")
    DevicePlatform platform,
    String deviceName,
    String osVersion,
    String appVersion

) {
}
