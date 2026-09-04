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
    public static MediaItem of(String url) {
        return new MediaItem(url, url, MediaType.IMAGE, null, null, null, null, null, null);
    }

    public static MediaItem of(String url, MediaType type) {
        return new MediaItem(url, url, type != null ? type : MediaType.IMAGE, null, null, null, null, null, null);
    }
}
