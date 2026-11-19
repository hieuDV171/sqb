package com.frozenheart.backend.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.media.MediaItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class LectureVideoUpload {
    private LectureVideoUpload() {}

    @Schema(description = "Metadata khởi tạo phiên video MP4/WebM; payload video được PUT trực tiếp lên R2")
    public record StartRequest(
            @Schema(description = "Tên tệp gốc do người dùng chọn, gồm phần mở rộng", example = "bai-giang-01.mp4")
            @NotBlank @Size(max = 255) String fileName,
            @Schema(description = "MIME video, chỉ video/mp4 hoặc video/webm", example = "video/mp4")
            @NotBlank @Size(max = 100) String contentType,
            @Schema(description = "Kích thước tệp byte, >0 và không vượt cấu hình LECTURE_VIDEO_MAX_FILE_SIZE (mặc định 2 GiB)", example = "104857600")
            @Positive long fileSize
    ) {}

    @Schema(description = "URL PUT có chữ ký dành cho đúng một phần của phiên multipart video")
    public record PartUrl(
            @Schema(description = "URL có chữ ký để PUT binary trực tiếp lên R2; không gửi Bearer SQB tới URL này", example = "https://r2.example.com/upload?signature=example")
            String uploadUrl) {}

    @Schema(description = "Trạng thái phiên multipart; media chỉ có sau khi object hoàn tất và được xác minh")
    public record Status(
            @Schema(description = "UUID do client sinh để định danh và tiếp tục phiên multipart", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,
            @Schema(description = "Kích thước chuẩn mỗi phần video theo byte; phần cuối có thể nhỏ hơn", example = "16777216")
            long partSize,
            @Schema(description = "Các số thứ tự phần đã tồn tại trên R2, bắt đầu từ 1", example = "[1,2]")
            List<Integer> uploadedParts,
            @Schema(description = "Media đã xác minh sau khi hoàn tất upload; null khi còn tải")
            MediaItem media) {}
}
