package com.frozenheart.backend.modules.session.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

import lombok.Builder;

@Builder
public record MySubmissionDetailResponse(
        Long sessionId,
        Long subjectId,
        String subjectName,
        int commentCount,
        int reactCount,
        LocalDateTime createdAt,
        List<MySubmissionQuestionDto> questions
) {
    @Builder
    public record MySubmissionQuestionDto(
            Long questionId,
            String content,
            List<String> imageUrls,
            List<QuestionOption> options,
            String explanation,
            String source,
            Double confidenceScore,
            String reviewedByLecturer,
            LocalDateTime reviewedAt,
            int commentCount,
            int reactCount,
            int ratingCount
    ) {}
}
