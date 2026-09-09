package com.frozenheart.backend.core.dto.event;

import java.util.Collections;
import java.util.List;

public record MediaCleanupEvent(
        List<String> urlsOrKeys
) {
    public static MediaCleanupEvent of(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return new MediaCleanupEvent(Collections.emptyList());
        }
        return new MediaCleanupEvent(List.of(urlOrKey));
    }

    public static MediaCleanupEvent of(List<String> urlsOrKeys) {
        if (urlsOrKeys == null || urlsOrKeys.isEmpty()) {
            return new MediaCleanupEvent(Collections.emptyList());
        }
        return new MediaCleanupEvent(urlsOrKeys);
    }
}
