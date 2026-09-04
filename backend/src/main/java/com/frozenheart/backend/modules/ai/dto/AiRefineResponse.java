package com.frozenheart.backend.modules.ai.dto;

import java.util.List;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import lombok.Builder;

@Builder
public record AiRefineResponse(
        Long editLogId,
        SuggestedQuestionDto suggestedQuestion,
        BtpropAuditDto hallucinationAudit,
        AiMetadataDto aiMetadata
) {
    @Builder
    public record SuggestedQuestionDto(
            String content,
            List<QuestionOption> options,
            String explanation,
            String refusalReason
    ) {}

    @Builder
    public record BtpropAuditDto(
            boolean isHallucinated,
            double confidenceScore,
            int beliefTreeDepth,
            List<ViolationDto> violations
    ) {}

    @Builder
    public record ViolationDto(
            String type,
            String nodeStatement,
            String detail,
            String severity
    ) {}
}
