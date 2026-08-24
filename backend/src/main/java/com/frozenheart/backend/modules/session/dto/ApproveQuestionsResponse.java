package com.frozenheart.backend.modules.session.dto;

import lombok.Builder;

@Builder
public record ApproveQuestionsResponse(
        double pointsEarned
) {}
