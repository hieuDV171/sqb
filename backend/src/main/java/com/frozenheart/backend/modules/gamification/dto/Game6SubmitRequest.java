package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record Game6SubmitRequest(
        @NotNull(message = "thiếu mã phiên game 6 (minigame_session_id)")
        Long minigameSessionId,

        @NotNull(message = "thiếu danh sách câu hỏi đã chọn")
        List<Long> selectedLlmQuestionIds
) {}
