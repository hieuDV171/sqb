package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import lombok.Builder;

@Builder
public record PendingSessionsResponse(
                List<PendingSessionItemDto> items,
                CursorPaginationDto pagination) {
        @Builder
        public record PendingSessionItemDto(
                        Long sessionId,
                        String topic,
                        String title,
                        String content,
                        AuthorDto author,
                        String status,
                        Instant createdAt) {
        }

        @Builder
        public record AuthorDto(
                        Long userId,
                        String fullName,
                        String avatarUrl,
                        String frameUrl) {
        }
}
