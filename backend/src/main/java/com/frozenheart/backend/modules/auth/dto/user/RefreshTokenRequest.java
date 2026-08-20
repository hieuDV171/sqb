package com.frozenheart.backend.modules.auth.dto.user;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "MISSING_REQUIRED_PARAMETER")
    String refreshToken
) {

}
