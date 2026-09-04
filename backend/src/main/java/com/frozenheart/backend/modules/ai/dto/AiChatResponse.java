package com.frozenheart.backend.modules.ai.dto;

import lombok.Builder;

@Builder
public record AiChatResponse(
        String aiResponse,
        String sessionId,
        AiMetadataDto aiMetadata
) {}
