package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import com.frozenheart.backend.core.entity.socialinteraction.QuestionErrorReport;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswer;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswerId;
import com.frozenheart.backend.core.entity.socialinteraction.UserRatingId;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.modules.session.dto.AnswerStatisticsProjection;
import com.frozenheart.backend.modules.session.dto.QuestionStatisticsResponse;
import com.frozenheart.backend.modules.session.dto.RateQuestionRequest;
import com.frozenheart.backend.modules.session.dto.RatingStatisticsProjection;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.QuestionErrorReportRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.UserAnswerRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.ReactRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionInteractionServiceImplTest {

    @Mock QuestionRepository questionRepository;
    @Mock SessionRepository sessionRepository;
    @Mock UserAnswerRepository userAnswerRepository;
    @Mock UserRatingRepository userRatingRepository;
    @Mock UserRepository userRepository;
    @Mock ReactRepository reactRepository;
    @Mock QuestionErrorReportRepository errorReportRepository;
    @Mock AnswerStatisticsProjection answerStatistics;
    @Mock RatingStatisticsProjection ratingStatistics;

    @InjectMocks QuestionInteractionServiceImpl service;

    @Test
    void submitsAReportWithoutCreatingARatingAndCanThenSaveOneStar() {
        long userId = 7L;
        long questionId = 101L;
        var authentication = UsernamePasswordAuthenticationToken.unauthenticated("student", null);
        authentication.setDetails(JwtPayload.builder().userId(userId).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var question = Question.builder().id(questionId)
                    .session(Session.builder().status(SessionStatus.RESOLVED).build()).build();
            var user = User.builder().id(userId).build();
            when(questionRepository.findByIdForFeedback(questionId)).thenReturn(Optional.of(question));
            when(userRepository.getReferenceById(userId)).thenReturn(user);

            service.reportQuestion(questionId, "  Thiếu dữ kiện  ");

            verifyNoInteractions(userRatingRepository);
            verify(errorReportRepository).save(argThat((QuestionErrorReport saved) ->
                    saved.getId().equals(new UserRatingId(userId, questionId))
                            && saved.getDescription().equals("Thiếu dữ kiện")));

            service.rateQuestion(questionId, RateQuestionRequest.builder().rating(0).build());

            verify(userRatingRepository).save(argThat(saved -> saved.getRating() == 0));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void keepsBothReviewOutcomesInPracticeAndLoadsOnlyTheViewersAnswer() {
        long userId = 7L;
        long sessionId = 12L;
        var authentication = UsernamePasswordAuthenticationToken.unauthenticated("student", null);
        authentication.setDetails(JwtPayload.builder().userId(userId).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var session = Session.builder().id(sessionId).status(SessionStatus.RESOLVED)
                    .courseClass(CourseClass.builder().build()).build();
            var first = Question.builder().id(101L).status(QuestionStatus.APPROVED).reactCount(4).build();
            var second = Question.builder().id(102L).status(QuestionStatus.REJECTED).reactCount(8).build();
            var selected = new QuestionOption();
            selected.setKey("B");
            selected.setText("Phương án đã lưu");
            var answer = UserAnswer.builder().id(new UserAnswerId(userId, 101L))
                    .selectedOptions(List.of(selected)).isCorrect(false).build();
            var reaction = new React();
            reaction.setTargetId(first.getId());
            reaction.setReactionType(ReactionType.LIKE);
            when(sessionRepository.findByIdFetchCourseClass(sessionId)).thenReturn(Optional.of(session));
            when(questionRepository.findQuestionsBySessionId(sessionId)).thenReturn(List.of(first, second));
            when(userAnswerRepository.findByUserIdAndQuestionIdIn(userId, List.of(101L, 102L)))
                    .thenReturn(List.of(answer));
            when(reactRepository.findUserReactsByTargetIds(userId, InteractionTargetType.QUESTION, List.of(101L, 102L)))
                    .thenReturn(List.of(reaction));

            var questions = service.getSessionQuestions(sessionId).questions();

            assertEquals(ReactionType.LIKE, questions.get(0).myInteraction().myReaction());
            assertEquals(4, questions.get(0).reactCount());
            assertEquals("B", questions.get(0).hiddenFields().selectedOptions().getFirst().key());
            assertEquals(Boolean.FALSE, questions.get(0).hiddenFields().wasCorrect());
            assertNull(questions.get(1).hiddenFields());
            assertNull(questions.get(1).myInteraction().myReaction());
            assertEquals(8, questions.get(1).reactCount());
            verify(reactRepository).findUserReactsByTargetIds(userId, InteractionTargetType.QUESTION, List.of(101L, 102L));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

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
