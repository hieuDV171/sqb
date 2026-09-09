package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record EditQuestionResponse(
                Long questionId,
                String status,
                Long reviewedBy,
                Instant reviewedAt) {
}
