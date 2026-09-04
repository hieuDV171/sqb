package com.frozenheart.backend.modules.exam.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionReportDto {
    private Integer order;
    private String questionContent;
    private List<String> questionImageUrls;
    private List<OptionReportDto> options;
    private String explanation;

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
