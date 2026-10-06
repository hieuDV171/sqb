package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Phạm vi bảng xếp hạng")
@Getter
@AllArgsConstructor
public enum LeaderboardPeriod {
    @Schema(description = "Bảng xếp hạng theo môn học cụ thể")
    SUBJECT("subject"),

    @Schema(description = "Bảng xếp hạng chung toàn học kỳ")
    SEMESTER("semester")
    ;
    private final String redisKey;
}

