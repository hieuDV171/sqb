package com.frozenheart.backend.modules.session.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record UpdateSubmissionSessionRequest(
        String title,
        String content,
        String sourceUrl,
        Long subjectId,
        @Valid List<QuestionUpdateDto> questions
) {
    @Builder
    public record QuestionUpdateDto(
            Long questionId,
            String content,
            List<String> mediaUrls,
            List<QuestionOption> options,

            String explanation,
            QuestionSource source,
            @Min(value = 0) @Max(value = 4) Double confidence
    ) {}
}
