package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record Game6ActiveSessionResponse(
        Long sessionId,
        int weekNumber,
        int year,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<Game6QuestionDto> questions
) {
    @Builder
    public record Game6QuestionDto(
            Long id,
            String content,
            List<QuestionOption> options,
            String explanation,
            List<String> imageUrls
    ) {}
}
