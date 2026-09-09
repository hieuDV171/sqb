package com.frozenheart.backend.modules.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {

    private Long examId;

    private String title;

    private String subjectName;

    private Integer questionCount;

    private List<ExamQuestionDto> questions;

    private Statistic statistic;

    private Instant createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamQuestionDto {
        private Integer order;

        private Long questionId;

        private String content;

        private List<String> imageUrls;

        private List<OptionDto> options;

        private DifficultyDto difficulty;

        private String topic;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String key;
        private String text;

        private String mediaUrl;

        private Boolean isCorrect;
    }

}
