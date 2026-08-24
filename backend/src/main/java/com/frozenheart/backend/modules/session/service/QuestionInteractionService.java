package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.AnswerQuestionRequest;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionResponse;
import com.frozenheart.backend.modules.session.dto.QuestionRatingsResponse;
import com.frozenheart.backend.modules.session.dto.QuestionStatisticsResponse;
import com.frozenheart.backend.modules.session.dto.RateQuestionRequest;
import com.frozenheart.backend.modules.session.dto.RateQuestionResponse;
import com.frozenheart.backend.modules.session.dto.UserQuestionsResponse;

public interface QuestionInteractionService {

    UserQuestionsResponse getUserProposedQuestions(Long userId, Long after, Integer limit, Long subjectId);

    AnswerQuestionResponse answerQuestion(Long questionId, AnswerQuestionRequest request);

    QuestionStatisticsResponse getQuestionStatistics(Long questionId);

    RateQuestionResponse rateQuestion(Long questionId, RateQuestionRequest request);

    QuestionRatingsResponse getQuestionRatings(Long questionId, Long after, Integer limit);

}
