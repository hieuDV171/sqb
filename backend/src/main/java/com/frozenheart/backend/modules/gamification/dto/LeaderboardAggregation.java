package com.frozenheart.backend.modules.gamification.dto;

import java.time.Instant;

public interface LeaderboardAggregation {

    Long getUserId();

    Double getTotalPoints();

    Instant getLastEventTime();

}
