package com.frozenheart.backend.modules.ai.dto;

import lombok.Builder;

@Builder
public record AiMetadataDto(
        String model,
        int tokensUsed,
        long processingTimeMs
    ) {
}