package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class ExamMatrix {
    private ExamMatrix() {}

    @Schema(name = "ExamDifficultyMatrix", description = "Bản chụp ma trận độ khó khi tạo đề")
    public record Difficulty(
            @Schema(description = "Tỷ lệ yêu cầu nếu có") GenerateExamRequest.DifficultyDistribution requested,
            @Schema(description = "Số câu thực tế theo độ khó") Statistic actual) {}

    @Schema(name = "ExamTopicMatrix", description = "Bản chụp ma trận chủ đề khi tạo đề")
    public record Topics(
            @Schema(description = "Tỷ lệ yêu cầu và số câu thực tế của từng chủ đề") List<TopicRow> rows) {}

    @Schema(name = "ExamTopicMatrixRow", description = "Hàng chủ đề trong ma trận đề thi")
    public record TopicRow(
            @Schema(description = "ID chủ đề nếu câu có chủ đề", example = "15") Long topicId,
            @Schema(description = "Tên chủ đề tại thời điểm tạo đề", example = "Giao thức HTTP") String topicName,
            @Schema(description = "Tỷ lệ yêu cầu nếu có", example = "0.5") Double requestedWeight,
            @Schema(description = "Số câu yêu cầu sau khi làm tròn, nếu áp dụng ma trận chủ đề", example = "10") Integer requestedCount,
            @Schema(description = "Tổng số câu thực tế trong đề của chủ đề", example = "10") int actualCount,
            @Schema(description = "Số câu thực tế theo độ khó trong chủ đề") Statistic actualByDifficulty) {}
}
