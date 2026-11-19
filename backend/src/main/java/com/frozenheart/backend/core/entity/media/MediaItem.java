package com.frozenheart.backend.core.entity.media;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata tệp đính kèm; key là object key, url để hiển thị, các kích thước có thể null khi không áp dụng")
public record MediaItem(
        @Schema(description = "Object key trong bucket; dùng key do API upload trả về để tham chiếu media", example = "lecture-videos/550e8400-e29b-41d4-a716-446655440000/video.mp4")
        String key,
        @Schema(description = "URL tài nguyên hoặc đích điều hướng theo schema", example = "https://media.example.com/document.pdf")
        String url,
        @Schema(description = "Loại media: IMAGE, VIDEO hoặc DOCUMENT")
        MediaType type,
        @Schema(description = "Chiều rộng ảnh/video theo pixel, null khi không áp dụng", example = "1280")
        Integer width,
        @Schema(description = "Chiều cao ảnh/video theo pixel, null khi không áp dụng", example = "720")
        Integer height,
        @Schema(description = "Dung lượng media theo byte", example = "1048576")
        Long size,
        @Schema(description = "Metadata thời lượng tùy chọn; không dùng trường này để xác minh dung lượng multipart", example = "300")
        Integer duration,
        @Schema(description = "Tên tệp gốc, có thể null", example = "bai-giang-01.mp4")
        String name,
        @Schema(description = "URL ảnh xem trước video/ảnh, có thể null khi không có", example = "https://media.example.com/thumbnail.jpg")
        String thumbnailUrl
) {
    public static MediaItem of(String url) {
        return new MediaItem(url, url, MediaType.IMAGE, null, null, null, null, null, null);
    }

    public static MediaItem of(String url, MediaType type) {
        return new MediaItem(url, url, type != null ? type : MediaType.IMAGE, null, null, null, null, null, null);
    }
}
