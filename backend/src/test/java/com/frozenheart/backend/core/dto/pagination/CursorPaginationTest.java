package com.frozenheart.backend.core.dto.pagination;

import com.frozenheart.backend.core.exception.AppException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CursorPaginationTest {
    private final List<Long> ids = List.of(41L, 39L, 28L, 20L, 12L);

    private IdCursorPage<Long> page(Long after, Long before) {
        return IdCursorPage.load(after, before, 2, id -> id, (next, previous, request) -> ids.stream()
                .filter(id -> (next == null || id < next) && (previous == null || id > previous))
                .sorted(request.getSort().getOrderFor("id").isAscending()
                        ? Comparator.naturalOrder() : Comparator.reverseOrder())
                .limit(request.getPageSize())
                .toList());
    }

    @Test
    void goesForwardAndBackAcrossThreePagesWithAccurateFlags() {
        var first = page(null, null);
        var middle = page(first.pagination().after(), null);
        var last = page(middle.pagination().after(), null);

        assertEquals(List.of(41L, 39L), first.items());
        assertFalse(first.pagination().hasPrev());
        assertEquals(List.of(28L, 20L), middle.items());
        assertTrue(middle.pagination().hasPrev());
        assertTrue(middle.pagination().hasNext());
        assertEquals(List.of(12L), last.items());
        assertFalse(last.pagination().hasNext());
        assertEquals(middle, page(null, last.pagination().before()));
        assertEquals(first, page(null, middle.pagination().before()));
    }

    @Test
    void emptyPageStillCanReturnAfterRecordsWereRemoved() {
        var empty = page(10L, null);

        assertTrue(empty.items().isEmpty());
        assertTrue(empty.pagination().hasPrev());
        assertEquals(List.of(20L, 12L), page(null, empty.pagination().before()).items());
    }

    @Test
    void timestampCursorPreservesPrecisionAndHandlesConversationsWithNoMessages() {
        Instant time = Instant.parse("2026-11-13T03:00:00.123456Z");
        assertEquals(new TimestampCursor(time, 103L, false), TimestampCursor.parse(TimestampCursor.encode(time, 103), null));
        assertEquals(new TimestampCursor(null, 202L, false), TimestampCursor.parse(TimestampCursor.encode(null, 202), null));
        assertThrows(AppException.class, () -> TimestampCursor.parse("bad|202", null));
        assertThrows(AppException.class, () -> page(28L, 39L));
    }
}
