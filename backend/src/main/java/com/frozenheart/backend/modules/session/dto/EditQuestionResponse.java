package com.frozenheart.backend.modules.session.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record EditQuestionResponse(
        Long questionId,
        String status,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {}
