package com.frozenheart.backend.modules.auth.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminResetPasswordRequest(
        @NotBlank(message = "MISSING_REQUIRED_PARAMETER") 
        @Email(message = "INVALID_PARAMETER_VALUE") 
        String email,

        String newPassword
) {

}
