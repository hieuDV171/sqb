package com.frozenheart.backend.core.entity.notification;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMetadata {

    private List<ConfirmedQuestionDto> confirmedQuestions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmedQuestionDto {
        private Long questionId;
        private String subjectName;
        private String questionSnippet;
        private double points;
    }
}
