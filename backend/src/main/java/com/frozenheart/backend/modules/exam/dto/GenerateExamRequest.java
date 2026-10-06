package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Schema(description = "Yêu cầu tạo đề thi tự động từ ngân hàng câu hỏi")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateExamRequest {

    @Schema(description = "ID môn học tạo đề", example = "10")
    @NotNull(message = "subject_id không được để trống")
    private Long subjectId;

    @Schema(description = "Tiêu đề đề thi (nếu để trống hệ thống sẽ tự sinh)", example = "Đề thi Giữa kỳ Hệ điều hành - Kỳ 2024.1")
    private String title;

    @Schema(description = "Số lượng câu hỏi trong đề thi", example = "40")
    @Builder.Default
    private Integer questionCount = 40;

    @Schema(description = "Tỉ lệ phân bổ độ khó các câu hỏi")
    private DifficultyDistribution difficultyDistribution;

    @Schema(description = "Trọng số các chủ đề/chương (tùy chọn)", example = "{\"Chương 1 - Tổng quan\": 0.2, \"Chương 2 - Quản lý tiến trình\": 0.5, \"Chương 3 - Định thời CPU\": 0.3}")
    private Map<String, Double> topicWeights;

    @Schema(description = "Đảo ngẫu nhiên thứ tự các phương án lựa chọn A, B, C, D", example = "true")
    @Builder.Default
    private Boolean shuffleOptions = false;

    @Schema(description = "Có xuất kèm bảng đáp án hay không", example = "false")
    @Builder.Default
    private Boolean includeAnswerKey = false;

    @Schema(description = "Danh sách ID các lớp học phần được áp dụng đề thi này", example = "[101, 102]")
    @NotEmpty(message = "Phải có ít nhất 1 id lớp học")
    private List<Long> classIds;

    @Schema(description = "Tỉ lệ phân bổ độ khó (tổng các giá trị tương đương 1.0)")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyDistribution {

        @Schema(description = "Tỉ lệ câu dễ (0.0 - 1.0)", example = "0.3")
        @Builder.Default
        private Double easy = 0.0;

        @Schema(description = "Tỉ lệ câu trung bình (0.0 - 1.0)", example = "0.5")
        @Builder.Default
        private Double medium = 0.0;

        @Schema(description = "Tỉ lệ câu khó (0.0 - 1.0)", example = "0.2")
        @Builder.Default
        private Double hard = 0.0;

        @Schema(description = "Tỉ lệ câu chưa phân loại", example = "0.0")
        @Builder.Default
        private Double unclassified = 1.0;
    }
}

