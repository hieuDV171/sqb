package com.frozenheart.backend.modules.media.dto;

import com.frozenheart.backend.core.entity.media.MediaType;

import lombok.Builder;

@Builder
public record MediaUploadResponse(
        String objectKey, // e.g. "questions/2026/08/550e8400-e29b.png"
        String url, // e.g. "https://cdn.domain.com/sqb-bucket/questions/2026/08/550e8400-e29b.png"
        Long fileSize, // Dung lượng byte
        String contentType, // e.g. "image/png"
        MediaType mediaType, // IMAGE, VIDEO, DOCUMENT
        Integer width, // null nếu không phải ảnh/video
        Integer height // null nếu không phải ảnh/video
) {

}
