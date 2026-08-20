package com.frozenheart.backend.modules.auth.dto.user;

import lombok.Builder;

@Builder
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {

}
