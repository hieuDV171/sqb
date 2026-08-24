package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record AnswerQuestionResponse(
        boolean isCorrect,
        String correctAnswer,
        String explanation
) {}
