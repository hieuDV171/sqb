package com.frozenheart.backend.modules.activityfeed.controller;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityFeedControllerTest {

    @Test
    void parsesEpochMilliseconds() {
        assertEquals(1_728_200_000_000L, ActivityFeedController.parseSince("1728200000000"));
    }

    @Test
    void parsesLegacyIsoTimestamp() {
        String timestamp = "2026-11-02T16:12:58.175Z";

        assertEquals(Instant.parse(timestamp).toEpochMilli(), ActivityFeedController.parseSince(timestamp));
    }

    @Test
    void rejectsInvalidTimestampAsBadRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> ActivityFeedController.parseSince("not-a-timestamp"));

        assertEquals(ResponseCode.INVALID_PARAMETER_VALUE, exception.getResponseCode());
    }

    @Test
    void acceptsMissingTimestamp() {
        assertNull(ActivityFeedController.parseSince(null));
        assertNull(ActivityFeedController.parseSince("   "));
    }
}
