package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin chi tiết đề thi")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {

    @Schema(description = "ID đề thi", example = "5")
    private Long examId;

    @Schema(description = "Tiêu đề đề thi", example = "Đề thi Giữa kỳ Hệ điều hành - Kỳ 2024.1")
    private String title;

    @Schema(description = "Tên môn học", example = "Hệ điều hành")
    private String subjectName;

    @Schema(description = "Tổng số lượng câu hỏi trong đề", example = "40")
    private Integer questionCount;

    @Schema(description = "Danh sách câu hỏi trong đề thi")
    private List<ExamQuestionDto> questions;

    @Schema(description = "Thống kê câu hỏi theo độ khó")
    private Statistic statistic;

    @Schema(description = "Thời gian tạo đề", example = "2026-10-06T09:00:00Z")
    private Instant createdAt;

    @Schema(description = "Chi tiết câu hỏi trong đề thi")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamQuestionDto {
        @Schema(description = "Thứ tự câu hỏi trong đề thi", example = "1")
        private Integer order;

        @Schema(description = "ID câu hỏi", example = "105")
        private Long questionId;

        @Schema(description = "Nội dung câu hỏi (hỗ trợ LaTeX/Markdown)", example = "Thuật toán định thời nào sau đây có thể dẫn đến starvation?")
        private String content;

        @Schema(description = "Danh sách URL hình ảnh minh họa cho câu hỏi")
        private List<String> imageUrls;

        @Schema(description = "Danh sách các lựa chọn đáp án")
        private List<OptionDto> options;

        @Schema(description = "Độ khó câu hỏi")
        private DifficultyDto difficulty;

        @Schema(description = "Tên chủ đề / chương", example = "Định thời CPU")
        private String topic;
    }

    @Schema(description = "Chi tiết phương án lựa chọn")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        @Schema(description = "Khóa phương án", example = "A")
        private String key;

        @Schema(description = "Nội dung phương án", example = "Thuật toán Round Robin (RR)")
        private String text;

        @Schema(description = "URL hình ảnh đáp án (nếu có)")
        private String mediaUrl;

        @Schema(description = "Đáp án này có đúng hay không", example = "false")
        private Boolean isCorrect;
    }

}

