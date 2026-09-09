package com.frozenheart.backend.modules.exam.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateExamRequest {

    @NotNull(message = "subject_id không được để trống")
    private Long subjectId;

    private String title;

    @Builder.Default
    private Integer questionCount = 40;

    private DifficultyDistribution difficultyDistribution;

    private Map<String, Double> topicWeights;

    @Builder.Default
    private Boolean shuffleOptions = false;

    @Builder.Default
    private Boolean includeAnswerKey = false;

    @NotEmpty(message = "Phải có ít nhất 1 id lớp học")
    private List<Long> classIds;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyDistribution {

        @Builder.Default
        private Double easy = 0.0;

        @Builder.Default
        private Double medium = 0.0;

        @Builder.Default
        private Double hard = 0.0;

        @Builder.Default
        private Double unclassified = 1.0;
    }
}
