package com.frozenheart.backend.modules.badge.dto;

import com.frozenheart.backend.core.entity.user.UserRole;

public record BadgeAccountResponse(Long userId, String fullName, String email, UserRole role) {
}
