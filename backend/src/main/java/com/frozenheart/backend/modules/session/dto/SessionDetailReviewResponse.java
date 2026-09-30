package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.QuestionOption;

import lombok.Builder;

@Builder
public record SessionDetailReviewResponse(
                List<SessionQuestionReviewDto> items,
                List<DuplicateWarning> duplicateWarnings) {
        @Builder
        public record SessionQuestionReviewDto(
                        Long questionId,
                        String content,
                        List<String> imageUrls,
                        List<QuestionOption> options,
                        String correctAnswer,
                        String explanation,
                        String status,
                        QuestionEditLogDto editLog) {
        }

        @Builder
        public record QuestionEditLogDto(
                        Long editLogId,
                        String actorType,
                        String actorName,
                        String status,
                        Object beforeState,
                        Object afterState,
                        Object hallucinationAudit,
                        Instant createdAt) {
        }
}
