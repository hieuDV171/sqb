package com.frozenheart.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LeaderboardPeriod {
    ALL_TIME("alltime"),
    SUBJECT("subject"),
    SEMESTER("semester")
    ;
    private final String redisKey;
}
