package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionOption;

import lombok.Builder;

@Builder
public record MySubmissionDetailResponse(
                Long sessionId,
                String sessionCode,
                String title,
                String content,
                Long subjectId,
                String subjectName,
                String subjectCode,
                int commentCount,
                int reactCount,
                String reviewedByLecturer,
                Instant reviewedAt,
                Instant createdAt,
                List<MySubmissionQuestionDto> questions) {
        @Builder
        public record MySubmissionQuestionDto(
                        Long questionId,
                        String content,
                        List<String> imageUrls,
                        List<QuestionOption> options,
                        String explanation,
                        String source,
                        Double confidenceScore,
                        int commentCount,
                        int reactCount,
                        int ratingCount) {
        }
}
