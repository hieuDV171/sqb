package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.gamification.repository.PredictionRepository;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository.Game3QuestionRating;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Game3ResolutionTest {
    private static final Instant SESSION_CLOSE = Instant.parse("2026-11-01T09:30:00Z");
    private static final Instant FIRST_DUE = SESSION_CLOSE.plus(14, ChronoUnit.DAYS);
    private static final Instant FINAL_DUE = SESSION_CLOSE.plus(28, ChronoUnit.DAYS);
    private static final long PREDICTION_ID = 50L;
    private static final PageRequest PAGE = PageRequest.of(0, 500);

    private final PredictionRepository predictions = mock(PredictionRepository.class);
    private final SemesterRepository semesters = mock(SemesterRepository.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final UserRatingRepository ratings = mock(UserRatingRepository.class);
    private final CounterMetricsService metrics = mock(CounterMetricsService.class);
    private final Game3ResolutionService service = new Game3ResolutionService(
            predictions, semesters, sessions, ratings, metrics);

    private final Semester semester = Semester.builder().id(3L).active(true).build();
    private final Subject subject = Subject.builder().id(4L).build();
    private final User author = User.builder().id(5L).build();
    private final Session session = Session.builder().id(6L).reviewedAt(SESSION_CLOSE).proposer(author)
            .courseClass(CourseClass.builder().semester(semester).subject(subject).build()).build();

    @Test
    void skipsSessionsWithFewerThanTenQuestions() {
        service.registerFinalizedSessions(Map.of(session, questions().subList(0, 9)));

        verifyNoInteractions(predictions, semesters, metrics);
    }

    @Test
    void snapshotsTheFirstTenByDisplayOrderIncludingRejectedQuestions() {
        Prediction prediction = registerPrediction();
        Map<?, ?> snapshot = (Map<?, ?>) prediction.getPredictionData().getData();

        assertEquals(10, snapshot.size());
        assertFalse(snapshot.containsKey("1"));
        assertEquals(2.5, snapshot.get("11"));
        assertEquals(FIRST_DUE, prediction.getTargetDate());
        assertEquals(PredictionStatus.PENDING, prediction.getStatus());
        verifyNoInteractions(metrics);
    }

    @Test
    void awardsOnceForFiveMatchesAtTheInclusiveDifferenceBoundary() {
        Prediction prediction = registerPrediction();
        mockDueBatch(prediction, FIRST_DUE);
        when(ratings.summarizeGame3Ratings(eq(Set.of(PREDICTION_ID)), anyCollection(), eq(FIRST_DUE)))
                .thenReturn(questionRatings(2, 6, 3, 3.0));
        when(sessions.findByIdInFetchSubjects(List.of(session.getId()))).thenReturn(List.of(session));

        service.resolveNextDueBatch(FIRST_DUE);
        service.resolveNextDueBatch(FIRST_DUE);

        assertEquals(PredictionStatus.RESOLVED, prediction.getStatus());
        assertTrue(prediction.isCorrect());
        assertEquals("WON", outcome(prediction));
        verify(metrics).awardGamePointsBatch(List.of(new UserPointRewardDto(
                author.getId(), 0.25, 0, PointHistoryReason.GAME_3_AUTHOR_CONFIDENCE_WIN,
                PointHistoryTargetType.SESSION, session.getId(), subject)), semester);
    }

    @Test
    void preservesTheSixMissesAndQuestionEvidenceAfterLaterRatingEdits() {
        Prediction prediction = registerPrediction();
        mockDueBatch(prediction, FIRST_DUE);
        mockDueBatch(prediction, FINAL_DUE);
        List<Game3QuestionRating> firstRatings = new ArrayList<>(questionRatings(2, 7, 3, 3.1));
        firstRatings.add(new RatingSummary(8, 2, 2.5));
        firstRatings.add(new RatingSummary(11, 0, null));
        when(ratings.summarizeGame3Ratings(eq(Set.of(PREDICTION_ID)), anyCollection(), any()))
                .thenReturn(firstRatings)
                .thenReturn(questionRatings(2, 11, 3, 2.5));

        service.resolveNextDueBatch(FIRST_DUE);

        assertEquals(PredictionStatus.RESOLVED, prediction.getStatus());
        assertFalse(prediction.isCorrect());
        assertEquals("LOST", outcome(prediction));
        assertEquals(FIRST_DUE, prediction.getResolvedAt());

        var mapper = JsonMapper.builder().build();
        var result = mapper.readTree(mapper.writeValueAsString(prediction.getActualData().getData()));
        assertEquals(6, result.path("nonMatchingQuestions").asInt());
        var evidence = result.path("questionEvaluations");
        assertEquals(10, evidence.size());
        var unrated = evidence.get(0);
        assertEquals(11, unrated.path("questionId").asLong());
        assertEquals(1, unrated.path("displayOrder").asInt());
        assertEquals("Q11", unrated.path("questionCode").asString());
        assertEquals(0, unrated.path("ratingCount").asLong());
        assertTrue(unrated.path("averageRating").isNull());
        assertEquals("INSUFFICIENT_RATINGS", unrated.path("verdict").asString());
        var mismatch = evidence.get(2);
        assertEquals(7, mismatch.path("questionId").asLong());
        assertEquals(2.5, mismatch.path("declaredConfidence").asDouble());
        assertEquals(3.1, mismatch.path("averageRating").asDouble());
        assertEquals(3, mismatch.path("ratingCount").asLong());
        assertEquals("MISMATCH", mismatch.path("verdict").asString());

        assertEquals(0, service.resolveNextDueBatch(FINAL_DUE));
        assertEquals(result, mapper.readTree(mapper.writeValueAsString(prediction.getActualData().getData())));
        verify(ratings).summarizeGame3Ratings(eq(Set.of(PREDICTION_ID)), anyCollection(), eq(FIRST_DUE));
        verifyNoInteractions(metrics);
    }

    @Test
    void extendsOnceAndRecordsAnUndeterminedResultAfterTheFinalDeadline() {
        Prediction prediction = registerPrediction();
        mockDueBatch(prediction, FIRST_DUE);
        when(ratings.summarizeGame3Ratings(eq(Set.of(PREDICTION_ID)), anyCollection(), any()))
                .thenReturn(questionRatings(2, 11, 2, 2.5));

        service.resolveNextDueBatch(FIRST_DUE);

        assertEquals(PredictionStatus.PENDING, prediction.getStatus());
        assertEquals(FINAL_DUE, prediction.getTargetDate());
        assertNull(prediction.getActualData());
        mockDueBatch(prediction, FINAL_DUE);

        service.resolveNextDueBatch(FINAL_DUE);

        assertEquals(PredictionStatus.RESOLVED, prediction.getStatus());
        assertEquals("INSUFFICIENT_RATINGS", outcome(prediction));
        verifyNoInteractions(metrics);
    }

    @Test
    void semesterFinalizationSettlesEvenWhenTheFirstDeadlineHasNotArrived() {
        Prediction prediction = registerPrediction();
        Instant earlyClose = SESSION_CLOSE.plus(7, ChronoUnit.DAYS);
        when(predictions.findPendingGame3Batch(semester.getId(), earlyClose, true, PAGE))
                .thenAnswer(invocation -> prediction.getStatus() == PredictionStatus.PENDING
                        ? List.of(prediction) : List.of());

        service.resolveBeforeSemesterClose(semester, earlyClose);

        assertEquals(PredictionStatus.RESOLVED, prediction.getStatus());
        assertEquals("INSUFFICIENT_RATINGS", outcome(prediction));
        assertEquals(earlyClose, prediction.getResolvedAt());
        verifyNoInteractions(metrics);
    }

    @Test
    void semesterFinalizationCanAwardAfterAnInconclusiveFirstEvaluation() {
        Prediction prediction = registerPrediction();
        Instant closing = SESSION_CLOSE.plus(20, ChronoUnit.DAYS);
        when(predictions.findPendingGame3Batch(semester.getId(), closing, true, PAGE))
                .thenAnswer(invocation -> prediction.getStatus() == PredictionStatus.PENDING
                        ? List.of(prediction) : List.of());
        when(ratings.summarizeGame3Ratings(eq(Set.of(PREDICTION_ID)), anyCollection(), eq(closing)))
                .thenAnswer(invocation -> prediction.getTargetDate().equals(FIRST_DUE)
                        ? questionRatings(2, 5, 3, 2.5) : questionRatings(2, 6, 3, 2.5));
        when(sessions.findByIdInFetchSubjects(List.of(session.getId()))).thenReturn(List.of(session));

        service.resolveBeforeSemesterClose(semester, closing);

        assertEquals("WON", outcome(prediction));
        assertEquals(closing, prediction.getResolvedAt());
        verify(metrics).awardGamePointsBatch(any(), eq(semester));
    }

    private Prediction registerPrediction() {
        AtomicReference<Prediction> registered = new AtomicReference<>();
        when(semesters.lockOpenSemesterIds(Set.of(semester.getId()))).thenReturn(List.of(semester.getId()));
        when(predictions.saveAll(any())).thenAnswer(invocation -> {
            List<Prediction> pending = invocation.getArgument(0);
            registered.set(pending.getFirst());
            return pending;
        });
        service.registerFinalizedSessions(Map.of(session, questions()));
        registered.get().setId(PREDICTION_ID);
        return registered.get();
    }

    private void mockDueBatch(Prediction prediction, Instant due) {
        when(semesters.findActiveForUpdate()).thenReturn(Optional.of(semester));
        when(predictions.findPendingGame3Batch(semester.getId(), due, false, PAGE)).thenAnswer(invocation ->
                prediction.getStatus() == PredictionStatus.PENDING && !prediction.getTargetDate().isAfter(due)
                        ? List.of(prediction) : List.of());
    }

    private static String outcome(Prediction prediction) {
        return (String) ((Map<?, ?>) prediction.getActualData().getData()).get("outcome");
    }

    private static List<Question> questions() {
        return LongStream.rangeClosed(1, 11).mapToObj(id -> Question.builder()
                .id(id).displayOrder(12 - (int) id).confidenceScore(2.5)
                .status(id == 11 ? QuestionStatus.REJECTED : QuestionStatus.APPROVED).build()).toList();
    }

    private static List<Game3QuestionRating> questionRatings(long first, long last, long count, double average) {
        return LongStream.rangeClosed(first, last)
                .<Game3QuestionRating>mapToObj(id -> new RatingSummary(id, count, average))
                .toList();
    }

    private record RatingSummary(long questionId, long count, Double average) implements Game3QuestionRating {
        @Override
        public Long getQuestionId() {
            return questionId;
        }

        @Override
        public String getQuestionCode() {
            return "Q" + questionId;
        }

        @Override
        public Integer getDisplayOrder() {
            return 12 - (int) questionId;
        }

        @Override
        public long getRatingCount() {
            return count;
        }

        @Override
        public Double getAverageRating() {
            return average;
        }
    }
}
