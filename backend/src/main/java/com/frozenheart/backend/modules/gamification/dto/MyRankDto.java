package com.frozenheart.backend.modules.gamification.dto;

import lombok.Builder;

@Builder
public record MyRankDto(
        int rank,
        double totalPoints,
        double topPercent,
        int totalParticipants,
        String fullName,
        String avatarUrl,
        String frameUrl
) {}
