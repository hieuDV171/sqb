package com.frozenheart.backend.modules.session.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;

import lombok.Builder;

@Builder
public record UserQuestionsResponse(
        List<UserQuestionItemDto> items,
        CursorPaginationDto pagination) {
    @Builder
    public record UserQuestionItemDto(
            Long questionId,
            String questionCode,
            String content,
            List<String> imageUrls,
            List<QuestionOption> options,
            QuestionSource source,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            MyInteractionDto myInteraction,
            HiddenFieldsDto hiddenFields,
            int reactCount,
            int commentCount,
            int ratingCount) {
    }

    @Builder
    public record MyInteractionDto(
            boolean answered,
            boolean rated) {
    }

    @Builder
    public record HiddenFieldsDto(
            String correctAnswer,
            String explanation) {
    }
}
