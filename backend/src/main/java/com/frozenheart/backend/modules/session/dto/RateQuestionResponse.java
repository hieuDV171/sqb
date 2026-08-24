package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record RateQuestionResponse(
        double newAvgRating,
        int newRatingCount
) {}
