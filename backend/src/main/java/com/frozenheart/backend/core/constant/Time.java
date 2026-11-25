package com.frozenheart.backend.core.constant;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Time {
    public static final long MAX_TIME_MS = 4096051200000L;
    public static final int DEFAULT_EXPIRATION_SECONDS = 7200;

    public static final DateTimeFormatter TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    private static final String LEGACY_VIETNAM_TIMEZONE = "Asia/Saigon";

    public static String canonicalTimezoneId(String timezone) {
        return LEGACY_VIETNAM_TIMEZONE.equals(timezone) ? DEFAULT_TIMEZONE : timezone;
    }

    public static String parseTimezoneId(String timezone) {
        String id = timezone.trim();
        if (LEGACY_VIETNAM_TIMEZONE.equals(id)) {
            throw new DateTimeException("Dùng " + DEFAULT_TIMEZONE + " cho Việt Nam");
        }
        return ZoneId.of(id).getId();
    }
}
