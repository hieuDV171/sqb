package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bộ câu hỏi có thể luyện tập sau khi chốt phiên, kèm phân trang con trỏ")
public record PracticeCatalogResponse(
        @Schema(description = "Các bộ câu hỏi trong trang danh mục luyện tập")
        List<PracticeSetDto> contents,
        @Schema(description = "Mốc và cờ phân trang; không tự suy đoán cursor từ thứ tự hiển thị")
        CursorPaginationDto pagination) {

    @Schema(description = "Thông tin bộ câu hỏi công khai cho người dùng đã đăng nhập")
    public record PracticeSetDto(
            @Schema(description = "ID phiên/bộ câu hỏi được liên kết, có thể null nếu không áp dụng", example = "42")
            Long sessionId,
            @Schema(description = "Mã bộ câu hỏi/phiên đóng góp", example = "SQB-2026-001")
            String sessionCode,
            @Schema(description = "Tiêu đề hiển thị", example = "Bộ câu hỏi ôn tập chương 1")
            String title,
            @Schema(description = "Nội dung văn bản theo ngữ cảnh của đối tượng", example = "Nội dung trao đổi về bài học")
            String content,
            @Schema(description = "Tên môn học", example = "Cơ sở dữ liệu")
            String subjectName,
            @Schema(description = "Mã môn học", example = "IT3290")
            String subjectCode,
            @Schema(description = "Số câu hỏi thuộc bộ câu hỏi", example = "3")
            Integer questionCounts,
            @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
            Instant createdAt,
            @Schema(description = "Tổng số lượt cảm xúc", example = "3")
            Integer reactCount,
            @Schema(description = "Cảm xúc hiện tại của người gọi; null nếu chưa thả cảm xúc")
            ReactionType myReaction,
            @Schema(description = "Tổng số bình luận hiển thị theo thống kê của đối tượng", example = "3")
            Integer commentCount) {
    }
}
