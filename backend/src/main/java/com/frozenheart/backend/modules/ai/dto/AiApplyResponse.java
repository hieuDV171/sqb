package com.frozenheart.backend.modules.ai.dto;

import lombok.Builder;

@Builder
public record AiApplyResponse(
        Long questionId,
        String status,
        double confidenceScore,
        boolean hallucination
) {}
