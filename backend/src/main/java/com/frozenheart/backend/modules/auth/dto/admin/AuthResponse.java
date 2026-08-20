package com.frozenheart.backend.modules.auth.dto.admin;

import lombok.Builder;

@Builder
public record AuthResponse(
        Long id,
        String username,
        String accessToken,
        String refreshToken,
        String avatarUrl,
        String coverUrl,
        String frameUrl,
        boolean verified,
        boolean profileCompleted

) {

}
