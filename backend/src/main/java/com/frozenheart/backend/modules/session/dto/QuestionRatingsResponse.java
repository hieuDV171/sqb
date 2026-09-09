package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import lombok.Builder;

@Builder
public record QuestionRatingsResponse(
        List<RatingItemDto> items,
        CursorPaginationDto pagination) {

    @Builder
    public record RatingItemDto(
            Long userId,
            double rating,
            boolean isError,
            String comment,
            Instant createdAt) {
    }
}
