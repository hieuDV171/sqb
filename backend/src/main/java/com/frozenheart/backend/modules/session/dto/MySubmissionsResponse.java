package com.frozenheart.backend.modules.session.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record MySubmissionsResponse(
        List<MySubmissionSessionSummaryDto> contents,
        CursorPaginationDto pagination
) {
    @Builder
    public record MySubmissionSessionSummaryDto(
            Long sessionId,
            Long subjectId,
            String subjectName,
            int questionCounts,
            LocalDateTime createdAt,
            int reactCount,
            int commentCount
    ) {}

    @Builder
    public record CursorPaginationDto(
            Long after,
            boolean hasNext
    ) {}
}
