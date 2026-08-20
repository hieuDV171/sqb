package com.frozenheart.backend.modules.auth.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String oldPassword,
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    @Size(min = 8, message = "INVALID_PARAMETER_VALUE")
    String newPassword
) {

}
