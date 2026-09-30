package com.frozenheart.backend.modules.ai.dto;

import java.time.Instant;

import lombok.Builder;

@Builder
public record AiChatHistoryResponse(
                Long messageId,
                String sessionId,
                String role,
                String content,
                Instant createdAt) {
}
