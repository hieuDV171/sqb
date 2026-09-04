package com.frozenheart.backend.modules.exam.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamQuestionReportDto {
    private Integer order;
    private String questionContent;
    private List<String> questionImageUrls;
    private String optionsText;
    private List<OptionReportDto> options;
    private String correctAnswer;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptionReportDto {
        private String key;
        private String text;
        private String mediaUrl;
        private Boolean isCorrect;
    }
}
