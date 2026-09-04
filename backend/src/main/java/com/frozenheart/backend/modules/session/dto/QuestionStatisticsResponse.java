package com.frozenheart.backend.modules.session.dto;

import java.util.Map;

import lombok.Builder;

@Builder
public record QuestionStatisticsResponse(
        long totalAnswer,
        double correctRate,
        Map<String, Long> optionDistribution,
        RatingSummaryDto ratingSummary
) {
    @Builder
    public record RatingSummaryDto(
            double avgRating,
            long totalRatings,
            Map<String, Long> distribution
    ) {}
}
