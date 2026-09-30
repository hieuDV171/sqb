package com.frozenheart.backend.modules.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record BtpropSidecarResponse(
        boolean isHallucinated,
        double confidenceScore,
        double riskScore,
        String riskLabel,
        int beliefTreeDepth,
        List<ViolationDto> violations,
        List<String> issues,
        List<String> likelySuitableOptions,
        String status,
        String mode
) {
    @Builder
    public record ViolationDto(
            String type,
            String nodeStatement,
            String detail,
            String severity
    ) {}
}
