package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GamePredictionResponse(
        Long id,
        GameType gameType,
        String targetType,
        Long targetId,
        Object predictionData,
        Object actualData,
        PredictionStatus status,
        boolean isCorrect,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}
