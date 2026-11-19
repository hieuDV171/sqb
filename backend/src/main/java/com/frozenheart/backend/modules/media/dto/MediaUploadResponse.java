package com.frozenheart.backend.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.media.MediaType;

import lombok.Builder;

@Builder
@Schema(description = "Kết quả tải tệp qua backend, gồm object key và metadata đã nhận diện")
public record MediaUploadResponse(
        @Schema(description = "Object key trong bucket để tham chiếu tệp, không phải đường dẫn filesystem", example = "documents/550e8400-e29b-41d4-a716-446655440000.pdf")
        String objectKey, // e.g. "questions/2026/08/550e8400-e29b.png"

        @Schema(description = "URL tài nguyên hoặc đích điều hướng theo schema", example = "https://media.example.com/document.pdf")
        String url, // e.g. "https://cdn.domain.com/sqb-bucket/questions/2026/08/550e8400-e29b.png"

        @Schema(description = "Dung lượng tệp đã tải lên tính bằng byte", example = "1048576")
        Long fileSize, // Dung lượng byte

        @Schema(description = "MIME type thực tế của tệp đã tải lên", example = "image/png")
        String contentType, // e.g. "image/png"

        @Schema(description = "Loại media được nhận diện, theo enum API")
        MediaType mediaType, // IMAGE, VIDEO, DOCUMENT

        @Schema(description = "Chiều rộng ảnh/video theo pixel, null khi không áp dụng", example = "1280")
        Integer width, // null nếu không phải ảnh/video

        @Schema(description = "Chiều cao ảnh/video theo pixel, null khi không áp dụng", example = "720")
        Integer height // null nếu không phải ảnh/video
) {

}
