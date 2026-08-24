package com.frozenheart.backend.modules.ai.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record AiChatHistoryResponse(
        Long id,
        String sessionId,
        String role,
        String content,
        LocalDateTime createdAt
) {}
