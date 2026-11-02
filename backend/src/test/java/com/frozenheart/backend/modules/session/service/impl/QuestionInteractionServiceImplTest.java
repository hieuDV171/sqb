package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.modules.session.dto.AnswerStatisticsProjection;
import com.frozenheart.backend.modules.session.dto.QuestionStatisticsResponse;
import com.frozenheart.backend.modules.session.dto.RatingStatisticsProjection;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.UserAnswerRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionInteractionServiceImplTest {

    @Mock QuestionRepository questionRepository;
    @Mock SessionRepository sessionRepository;
    @Mock UserAnswerRepository userAnswerRepository;
    @Mock UserRatingRepository userRatingRepository;
    @Mock UserRepository userRepository;
    @Mock AnswerStatisticsProjection answerStatistics;
    @Mock RatingStatisticsProjection ratingStatistics;

    @InjectMocks QuestionInteractionServiceImpl service;

    @Test
    void mapsAggregateProjectionsWithoutObjectArrayCasts() {
        long questionId = 101L;
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(Question.builder().id(questionId).build()));
        when(userAnswerRepository.getAnswerStatsSummary(questionId)).thenReturn(answerStatistics);
        when(answerStatistics.getTotalAnswers()).thenReturn(10L);
        when(answerStatistics.getCorrectAnswers()).thenReturn(7L);
        when(userAnswerRepository.findSelectedOptionsByQuestionId(questionId)).thenReturn(List.of());
        when(userRatingRepository.getRatingStatsSummary(questionId)).thenReturn(ratingStatistics);
        when(ratingStatistics.getAvgRating()).thenReturn(3.5);
        when(ratingStatistics.getTotalRatings()).thenReturn(6L);
        when(ratingStatistics.getRating0Count()).thenReturn(1L);
        when(ratingStatistics.getRating1Count()).thenReturn(0L);
        when(ratingStatistics.getRating2Count()).thenReturn(1L);
        when(ratingStatistics.getRating3Count()).thenReturn(2L);
        when(ratingStatistics.getRating4Count()).thenReturn(2L);

        QuestionStatisticsResponse response = service.getQuestionStatistics(questionId);

        assertEquals(10L, response.totalAnswer());
        assertEquals(0.7, response.correctRate());
        assertEquals(3.5, response.ratingSummary().avgRating());
        assertEquals(6L, response.ratingSummary().totalRatings());
        assertEquals(2L, response.ratingSummary().distribution().get("4"));
    }
}
