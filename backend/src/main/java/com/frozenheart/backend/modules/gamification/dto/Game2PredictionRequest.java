package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.constraints.NotNull;

public record Game2PredictionRequest(
        @NotNull(message = "session_id (mã phiên) không được bỏ trống")
        Long sessionId,

        Integer predictedLlmCount,
        Integer predictedHumanCount
) {}
