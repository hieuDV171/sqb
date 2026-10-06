package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Yêu cầu tái xây dựng lại Bảng xếp hạng trong Redis")
public record RebuildLeaderboardRequest(
        @Schema(description = "Loại phạm vi bảng xếp hạng", example = "SEMESTER")
        LeaderboardPeriod type,

        @Schema(description = "ID môn học (nếu tái tạo bảng xếp hạng theo môn)", example = "10")
        Long subjectId
) {
}

