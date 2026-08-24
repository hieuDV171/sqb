package com.frozenheart.backend.core.dto.pagination;

import java.util.List;

import lombok.Builder;

@Builder
public record CursorResponse<T>(
        List<T> items,
        CursorPaginationDto pagination
) {}
