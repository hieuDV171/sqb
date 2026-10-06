package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Phản hồi danh sách đánh giá của câu hỏi")
@Builder
public record QuestionRatingsResponse(
        @Schema(description = "Danh sách chi tiết các đánh giá")
        List<RatingItemDto> items,

        @Schema(description = "Thông tin phân trang dạng con trỏ")
        CursorPaginationDto pagination) {

    @Schema(description = "Chi tiết một lượt đánh giá câu hỏi")
    @Builder
    public record RatingItemDto(
            @Schema(description = "ID người đánh giá", example = "55")
            Long userId,

            @Schema(description = "Điểm số đánh giá [0; 4]", example = "4.0")
            double rating,

            @Schema(description = "Có gắn cờ báo lỗi nội dung câu hỏi hay không", example = "false")
            boolean isError,

            @Schema(description = "Nhận xét của người đánh giá", example = "Câu hỏi hay, phân loại tốt!")
            String comment,

            @Schema(description = "Thời gian đánh giá", example = "2026-10-06T11:20:00Z")
            Instant createdAt) {
    }
}

