package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Danh sách phiên đề xuất câu hỏi của người dùng (kèm phân trang con trỏ)")
public record SubmissionsResponse(
        @Schema(description = "Danh sách các phiên đề xuất")
        List<SubmissionSessionSummaryDto> contents,

        @Schema(description = "Thông tin phân trang theo con trỏ cursor")
        CursorPaginationDto pagination
    ) {
    @Builder
    @Schema(description = "Thông tin tóm tắt của một phiên nộp đề xuất")
    public record SubmissionSessionSummaryDto(
            @Schema(description = "ID phiên đề xuất", example = "101")
            Long sessionId,

            @Schema(description = "Mã phiên nộp duy nhất", example = "IT3180_A7B2C1_1728200000000_101")
            String sessionCode,

            @Schema(description = "Tiêu đề phiên nộp", example = "Ôn tập Design Patterns")
            String title,

            @Schema(description = "Mô tả nội dung phiên", example = "5 câu trắc nghiệm Saga & CQRS")
            String content,

            @Schema(description = "ID môn học", example = "10")
            Long subjectId,

            @Schema(description = "Tên môn học", example = "Nhập môn Công nghệ phần mềm")
            String subjectName,

            @Schema(description = "Mã môn học", example = "IT3180")
            String subjectCode,

            @Schema(description = "Số lượng câu hỏi trong phiên", example = "5")
            int questionCounts,

            @Schema(description = "Thời điểm khởi tạo phiên nộp")
            Instant createdAt,

            @Schema(description = "Số lượt thả cảm xúc (react) cho phiên", example = "12")
            int reactCount,

            @Schema(description = "Số lượng bình luận trao đổi trong phiên", example = "4")
            int commentCount
        ) {
    }
}
