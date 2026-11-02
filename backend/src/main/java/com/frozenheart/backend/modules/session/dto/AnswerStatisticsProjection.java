package com.frozenheart.backend.modules.session.dto;

public interface AnswerStatisticsProjection {
    Long getTotalAnswers();

    Long getCorrectAnswers();
}
