package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.prediction.PredictionTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Schema(description = "Thông tin chi tiết một lượt dự đoán minigame")
@Builder
public record GamePredictionResponse(
        @Schema(description = "ID lượt dự đoán", example = "1001")
        Long predictionId,

        @Schema(description = "Loại trò chơi dự đoán", example = "GAME_1_PARTICIPANTS")
        GameType gameType,

        @Schema(description = "Loại đối tượng dự đoán", example = "COURSE_CLASS")
        PredictionTargetType targetType,

        @Schema(description = "ID đối tượng dự đoán", example = "101")
        Long targetId,

        @Schema(description = "Dữ liệu người chơi đã dự đoán")
        Object predictionData,

        @Schema(description = "Dữ liệu kết quả thực tế sau khi tổng kết")
        Object actualData,

        @Schema(description = "Trạng thái lượt dự đoán", example = "PENDING")
        PredictionStatus status,

        @Schema(description = "Dự đoán chính xác hay không", example = "false")
        boolean isCorrect,

        @Schema(description = "Thời điểm đặt dự đoán", example = "2026-10-06T15:00:00Z")
        Instant createdAt,

        @Schema(description = "Thời điểm tổng kết kết quả", example = "2026-10-07T00:05:00Z")
        Instant resolvedAt
    ) {
}

