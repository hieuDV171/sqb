package com.frozenheart.backend.modules.gamification.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

public record CandidateQuestion(
        Long originalId,
        BankType sourceType,
        String content,
        List<QuestionOption> options,
        String explanation,
        List<String> imageUrls
    ) {
}