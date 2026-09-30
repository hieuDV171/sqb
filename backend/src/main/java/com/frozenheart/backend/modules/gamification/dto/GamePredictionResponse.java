package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.prediction.PredictionTargetType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record GamePredictionResponse(
        Long predictionId,
        GameType gameType,
        PredictionTargetType targetType,
        Long targetId,
        Object predictionData,
        Object actualData,
        PredictionStatus status,
        boolean isCorrect,
        Instant createdAt,
        Instant resolvedAt
    ) {
}
