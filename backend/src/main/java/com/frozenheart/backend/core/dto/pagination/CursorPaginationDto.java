package com.frozenheart.backend.core.dto.pagination;

import lombok.Builder;

@Builder
public record CursorPaginationDto(
        Long after,
        boolean hasNext
) {}
