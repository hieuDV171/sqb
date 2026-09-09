package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import lombok.Builder;

@Builder
public record MySubmissionsResponse(
                List<MySubmissionSessionSummaryDto> contents,
                CursorPaginationDto pagination) {
        @Builder
        public record MySubmissionSessionSummaryDto(
                        Long sessionId,
                        Long subjectId,
                        String subjectName,
                        String subjectCode,
                        int questionCounts,
                        Instant createdAt,
                        int reactCount,
                        int commentCount) {
        }
}
