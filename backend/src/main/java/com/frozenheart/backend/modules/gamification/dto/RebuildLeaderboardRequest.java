package com.frozenheart.backend.modules.gamification.dto;

public record RebuildLeaderboardRequest(
        LeaderboardPeriod type,
        Long subjectId
) {
}
