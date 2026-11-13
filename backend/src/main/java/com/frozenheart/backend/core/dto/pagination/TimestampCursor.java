package com.frozenheart.backend.core.dto.pagination;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public record TimestampCursor(Instant timestamp, Long id, boolean firstPage) {
    public static TimestampCursor parse(String cursor, Long legacyAfter) {
        if (cursor == null) {
            boolean firstPage = legacyAfter == null || legacyAfter <= 0;
            return new TimestampCursor(firstPage ? Instant.EPOCH : Instant.ofEpochMilli(legacyAfter), null, firstPage);
        }
        int separator = cursor.indexOf('|');
        if (separator < 0) throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
        try {
            long id = Long.parseLong(cursor.substring(separator + 1));
            if (id <= 0) throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
            Instant timestamp = separator == 0 ? null : Instant.parse(cursor.substring(0, separator));
            return new TimestampCursor(timestamp, id, false);
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
        }
    }

    public static String encode(Instant timestamp, long id) {
        return (timestamp == null ? "" : timestamp.toString()) + "|" + id;
    }
}
