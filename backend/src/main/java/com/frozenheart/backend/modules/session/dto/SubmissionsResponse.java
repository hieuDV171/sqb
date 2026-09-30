package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import lombok.Builder;

@Builder
public record SubmissionsResponse(
        List<SubmissionSessionSummaryDto> contents,
        CursorPaginationDto pagination
    ) {
    @Builder
    public record SubmissionSessionSummaryDto(
            Long sessionId,
            String sessionCode,
            String title,
            String content,
            Long subjectId,
            String subjectName,
            String subjectCode,
            int questionCounts,
            Instant createdAt,
            int reactCount,
            int commentCount
        ) {
    }
}
