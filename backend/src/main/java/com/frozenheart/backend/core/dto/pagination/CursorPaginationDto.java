package com.frozenheart.backend.core.dto.pagination;

import lombok.Builder;

@Builder
public record CursorPaginationDto(
        Long before,
        Long after,
        boolean hasPrev,
        boolean hasNext
) {}
