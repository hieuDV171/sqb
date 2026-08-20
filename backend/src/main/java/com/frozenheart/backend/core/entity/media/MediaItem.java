package com.frozenheart.backend.core.entity.media;

public record MediaItem(
        String key,
        String url,
        MediaType type,
        Integer width,
        Integer height,
        Long size,
        Integer duration,
        String name,
        String thumbnailUrl

) {
}
