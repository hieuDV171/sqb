package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Độ khó của câu hỏi")
public enum DifficultyDto {
    @Schema(description = "Chưa phân loại")
    UNCLASSIFIED,
    @Schema(description = "Dễ")
    EASY,
    @Schema(description = "Trung bình")
    MEDIUM,
    @Schema(description = "Khó")
    HARD
}

