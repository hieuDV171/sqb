package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

import lombok.Builder;

@Builder
public record EditQuestionRequest(
        String content,
        List<String> imageUrls,
        List<QuestionOption> options,
        String correctAnswer,
        String explanation,
        Boolean autoApprove
) {}
