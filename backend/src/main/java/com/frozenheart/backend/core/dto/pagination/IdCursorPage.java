package com.frozenheart.backend.core.dto.pagination;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

public record IdCursorPage<T>(List<T> items, CursorPaginationDto pagination) {
    @FunctionalInterface
    public interface Query<T> {
        List<T> load(Long after, Long before, Pageable pageable);
    }

    public static <T> IdCursorPage<T> load(Long after, Long before, int limit, Function<T, Long> id, Query<T> query) {
        if ((after != null && after <= 0) || (before != null && before <= 0) || (after != null && before != null)) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
        }
        boolean backwards = before != null;
        Sort descending = Sort.by(Sort.Direction.DESC, "id");
        Sort ascending = Sort.by(Sort.Direction.ASC, "id");
        List<T> rows = query.load(after, before, PageRequest.of(0, limit + 1, backwards ? ascending : descending));
        boolean extra = rows.size() > limit;
        List<T> items = extra ? rows.subList(0, limit) : rows;
        if (backwards) items = items.reversed();

        Long first = items.isEmpty() ? after : id.apply(items.getFirst());
        Long last = items.isEmpty() ? before : id.apply(items.getLast());
        boolean hasPrev = backwards ? extra : after != null && first != null
                && !query.load(null, first, PageRequest.of(0, 1, ascending)).isEmpty();
        boolean hasNext = backwards ? last != null
                && !query.load(last, null, PageRequest.of(0, 1, descending)).isEmpty() : extra;
        return new IdCursorPage<>(items, CursorPaginationDto.builder()
                .hasPrev(hasPrev).before(hasPrev ? first : null)
                .hasNext(hasNext).after(hasNext ? last : null).build());
    }
}
