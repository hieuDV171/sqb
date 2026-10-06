package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Yêu cầu nộp dự đoán Game 6: Phán đoán câu hỏi do AI tạo")
public record Game6SubmitRequest(
        @Schema(description = "ID phiên minigame Game 6", example = "3")
        @NotNull(message = "thiếu mã phiên game 6 (minigame_session_id)")
        Long minigameSessionId,

        @Schema(description = "Danh sách ID các câu hỏi mà bạn phán đoán do AI tạo", example = "[105, 107]")
        @NotNull(message = "thiếu danh sách câu hỏi đã chọn")
        List<Long> selectedLlmQuestionIds
) {}

