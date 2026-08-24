package com.frozenheart.backend.modules.ai.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record AiChatSessionSummaryResponse(
        Long id,
        String sessionId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt
) {}
