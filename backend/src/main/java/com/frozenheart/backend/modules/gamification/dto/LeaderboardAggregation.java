package com.frozenheart.backend.modules.gamification.dto;

import java.time.LocalDateTime;

public interface LeaderboardAggregation {

    Long getUserId();
    Double getTotalPoints();
    LocalDateTime getLastEventTime();

}
