package com.frozenheart.backend.modules.exam.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Schema(description = "Phản hồi danh sách đề thi của giảng viên")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamListResponse {

    @Schema(description = "Danh sách đề thi")
    private List<ExamItem> items;

    @Schema(description = "Thông tin phân trang dạng con trỏ")
    private CursorPaginationDto pagination;

    @Schema(description = "Thông tin tóm tắt đề thi")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamItem {
        @Schema(description = "ID đề thi", example = "5")
        private Long examId;

        @Schema(description = "Tiêu đề đề thi", example = "Đề thi Giữa kỳ Hệ điều hành")
        private String title;

        @Schema(description = "Tên môn học", example = "Hệ điều hành")
        private String subjectName;

        @Schema(description = "Tổng số câu hỏi trong đề", example = "40")
        private Integer questionCount;

        @Schema(description = "Thời gian tạo đề thi", example = "2026-10-06T09:00:00Z")
        private Instant createdAt;

        @Schema(description = "Thống kê câu hỏi theo độ khó")
        private Statistic statistic;

        @Schema(description = "URL tải file PDF đề thi (nếu có)")
        private String downloadUrl;
    }

}

