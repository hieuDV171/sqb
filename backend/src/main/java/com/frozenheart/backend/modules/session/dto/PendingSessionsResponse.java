package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Phản hồi danh sách các phiên đề xuất câu hỏi đang chờ duyệt")
@Builder
public record PendingSessionsResponse(
        @Schema(description = "Danh sách các phiên chờ duyệt")
        List<PendingSessionItemDto> items,

        @Schema(description = "Thông tin phân trang dạng con trỏ")
        CursorPaginationDto pagination) {

    @Schema(description = "Thông tin tóm tắt phiên đề xuất chờ duyệt")
    @Builder
    public record PendingSessionItemDto(
            @Schema(description = "ID của phiên đề xuất", example = "12")
            Long sessionId,

            @Schema(description = "Tên môn học / học phần", example = "Hệ điều hành")
            String topic,

            @Schema(description = "Tiêu đề phiên đề xuất", example = "Đề xuất câu hỏi trắc nghiệm Chương 3")
            String title,

            @Schema(description = "Mô tả nội dung phiên", example = "Bộ câu hỏi trắc nghiệm về giải thuật điều phối CPU")
            String content,

            @Schema(description = "Thông tin tác giả đề xuất (đã được ẩn danh hóa)")
            AuthorDto author,

            @Schema(description = "Trạng thái phiên đề xuất", example = "PENDING")
            String status,

            @Schema(description = "Thời gian tạo phiên đề xuất", example = "2026-10-06T08:30:00Z")
            Instant createdAt) {
    }

    @Schema(description = "Thông tin ẩn danh của tác giả đề xuất")
    @Builder
    public record AuthorDto(
            @Schema(description = "ID tác giả", example = "42")
            Long userId,

            @Schema(description = "Tên hiển thị ẩn danh của học viên", example = "A8F2C1")
            String fullName,

            @Schema(description = "URL ảnh đại diện", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/avatars/user42.png")
            String avatarUrl,

            @Schema(description = "URL khung viền đại diện", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/frames/top1.png")
            String frameUrl) {
    }
}

