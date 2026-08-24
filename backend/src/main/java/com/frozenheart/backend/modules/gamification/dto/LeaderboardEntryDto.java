package com.frozenheart.backend.modules.gamification.dto;

import lombok.Builder;

@Builder
public record LeaderboardEntryDto(
        int rank,
        Long userId,
        String userCode,
        String fullName,
        String avatarUrl,
        String frameUrl,
        double totalPoints,
        boolean isCurrentUser
) {}
