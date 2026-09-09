package com.frozenheart.backend.modules.ai.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record AiChatSessionSummaryResponse(
                Long id,
                String sessionId,
                String title,
                Instant createdAt,
                Instant lastActiveAt) {
}
