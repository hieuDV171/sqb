package com.frozenheart.backend.modules.auth.dto.admin;

import com.frozenheart.backend.core.entity.user.UserRole;
import lombok.Builder;

@Builder
public record AuthResponse(
        Long userId,
        String username,
        UserRole role,
        String accessToken,
        String refreshToken,
        String avatarUrl,
        String coverUrl,
        String frameUrl,
        boolean verified,
        boolean profileCompleted

) {

}
