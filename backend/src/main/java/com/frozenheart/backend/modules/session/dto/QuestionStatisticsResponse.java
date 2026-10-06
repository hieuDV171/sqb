package com.frozenheart.backend.modules.session.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Thống kê chi tiết tương tác của câu hỏi")
@Builder
public record QuestionStatisticsResponse(
        @Schema(description = "Tổng số lượt người dùng đã trả lời", example = "150")
        long totalAnswer,

        @Schema(description = "Tỷ lệ trả lời chính xác (0.0 - 1.0)", example = "0.78")
        double correctRate,

        @Schema(description = "Phân bố số lượng lựa chọn cho từng đáp án A, B, C, D", example = "{\"A\": 117, \"B\": 15, \"C\": 10, \"D\": 8}")
        Map<String, Long> optionDistribution,

        @Schema(description = "Tóm tắt thống kê đánh giá chất lượng câu hỏi")
        RatingSummaryDto ratingSummary
) {
    @Schema(description = "Tóm tắt đánh giá chất lượng câu hỏi")
    @Builder
    public record RatingSummaryDto(
            @Schema(description = "Điểm đánh giá trung bình", example = "3.85")
            double avgRating,

            @Schema(description = "Tổng số lượt đánh giá", example = "42")
            long totalRatings,

            @Schema(description = "Phân bố số lượt đánh giá theo thang điểm từ 0 đến 4", example = "{\"0\": 1, \"1\": 2, \"2\": 4, \"3\": 15, \"4\": 20}")
            Map<String, Long> distribution
    ) {}
}

