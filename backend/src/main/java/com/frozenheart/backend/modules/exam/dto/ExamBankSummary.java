package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Số lượng câu hỏi trong ngân hàng có thể dùng để tạo đề của một môn")
public record ExamBankSummary(
        @Schema(description = "ID môn học", example = "12") Long subjectId,
        @Schema(description = "Tổng số câu đã duyệt trong phiên đã chốt", example = "72") int totalQuestions,
        @Schema(description = "Số câu theo độ khó của toàn môn") Statistic statistic,
        @Schema(description = "Số câu của từng chủ đề đang hoạt động") List<TopicStock> topics) {

    @Schema(name = "ExamBankTopicStock", description = "Số lượng câu hỏi trong ngân hàng của một chủ đề")
    public record TopicStock(
            @Schema(description = "ID chủ đề", example = "15") Long topicId,
            @Schema(description = "Tên chủ đề", example = "Giao thức HTTP") String topicName,
            @Schema(description = "Tổng số câu của chủ đề", example = "24") int totalQuestions,
            @Schema(description = "Số câu mỗi mức độ khó trong chủ đề") Statistic statistic) {}
}
