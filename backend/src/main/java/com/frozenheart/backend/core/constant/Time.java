package com.frozenheart.backend.core.constant;

import java.time.format.DateTimeFormatter;

public class Time {
    public static final long MAX_TIME_MS = 4096051200000L;
    public static final int DEFAULT_EXPIRATION_SECONDS = 7200;

    public static final DateTimeFormatter TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
}
