package com.frozenheart.backend.modules.gamification.dto;

import lombok.Builder;

@Builder
public record LeaderboardWinnerEntry(
        Long userId,
        int rank,
        double points,
        double coins
) {}
