package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;

import lombok.Builder;

@Builder
public record SessionQuestionsResponse(
        Long sessionId,
        String sessionCode,
        String title,
        String content,
        Long subjectId,
        String subjectName,
        String subjectCode,
        Instant createdAt,
        int totalQuestions,
        List<PracticeQuestionDto> questions
    ) {
    @Builder
    public record PracticeQuestionDto(
            Long questionId,
            String questionCode,
            String content,
            List<String> imageUrls,
            List<QuestionOption> options,
            QuestionSource source,
            Instant createdAt,
            Instant updatedAt,
            MyInteractionDto myInteraction,
            HiddenFieldsDto hiddenFields,
            int reactCount,
            int commentCount,
            int ratingCount
        ) {
    }

    @Builder
    public record MyInteractionDto(
            boolean answered,
            boolean rated
        ) {
    }

    @Builder
    public record HiddenFieldsDto(
            String correctAnswer,
            String explanation
        ) {
    }
}
