package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.prediction.PredictionData;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.prediction.PredictionTargetType;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.modules.gamification.repository.PredictionRepository;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository.Game3QuestionRating;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Game3ResolutionService {
    private static final int QUESTION_COUNT = 10;
    private static final int MINIMUM_RATERS = 3;
    private static final int MATCHES_TO_WIN = 5;
    private static final int MISSES_TO_LOSE = QUESTION_COUNT - MATCHES_TO_WIN + 1;
    private static final int FIRST_REVIEW_DAYS = 14;
    private static final int FINAL_REVIEW_DAYS = 28;
    private static final int BATCH_SIZE = 500;
    private static final double MAX_RATING_DIFFERENCE = 0.5;
    private static final Comparator<Question> SUBMISSION_ORDER = Comparator
            .comparing(Question::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Question::getId);

    private final PredictionRepository predictionRepository;
    private final SemesterRepository semesterRepository;
    private final SessionRepository sessionRepository;
    private final UserRatingRepository userRatingRepository;
    private final CounterMetricsService counterMetricsService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registerFinalizedSessions(Map<Session, List<Question>> finalizedSessions) {
        List<Session> eligible = finalizedSessions.keySet().stream()
                .filter(session -> session.getProposer() != null && session.getReviewedAt() != null
                        && finalizedSessions.get(session).size() >= QUESTION_COUNT)
                .toList();
        if (eligible.isEmpty()) {
            return;
        }

        // Both callers hold the review session locks and only pass sessions just transitioned to RESOLVED.
        Set<Long> semesterIds = eligible.stream().map(this::semesterId).collect(Collectors.toSet());
        Set<Long> openSemesters = Set.copyOf(semesterRepository.lockOpenSemesterIds(semesterIds));
        List<Prediction> pending = eligible.stream()
                .filter(session -> openSemesters.contains(semesterId(session)))
                .map(session -> createPrediction(session, finalizedSessions.get(session)))
                .toList();
        predictionRepository.saveAll(pending);
    }

    private Long semesterId(Session session) {
        return session.getCourseClass().getSemester().getId();
    }

    private Prediction createPrediction(Session session, List<Question> questions) {
        Map<String, Double> confidenceByQuestion = new LinkedHashMap<>();
        questions.stream().sorted(SUBMISSION_ORDER).limit(QUESTION_COUNT).forEach(question ->
                confidenceByQuestion.put(question.getId().toString(), question.getConfidenceScore()));

        Prediction prediction = new Prediction();
        prediction.setGambler(session.getProposer());
        prediction.setGameType(GameType.GAME_3_AUTHOR_CONFIDENCE);
        prediction.setTargetType(PredictionTargetType.SESSION);
        prediction.setTargetId(session.getId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(session.getReviewedAt());
        prediction.setTargetDate(session.getReviewedAt().plus(FIRST_REVIEW_DAYS, ChronoUnit.DAYS));
        prediction.setPredictionData(new PredictionData(confidenceByQuestion));
        return prediction;
    }

    @Transactional
    public int resolveNextDueBatch(Instant now) {
        return semesterRepository.findActiveForUpdate()
                .filter(semester -> !semester.isFinalized())
                .map(semester -> resolveBatch(semester, now, false))
                .orElse(0);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void resolveBeforeSemesterClose(Semester lockedSemester, Instant cutoff) {
        while (resolveBatch(lockedSemester, cutoff, true) > 0) {
            // Each pass settles or advances every prediction it reads.
        }
    }

    private int resolveBatch(Semester semester, Instant now, boolean closingSemester) {
        List<Prediction> batch = predictionRepository.findPendingGame3Batch(
                semester.getId(), now, closingSemester, PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) {
            return 0;
        }

        Map<Long, Map<?, ?>> confidenceByPrediction = new HashMap<>();
        List<Long> questionIds = new ArrayList<>();
        for (Prediction prediction : batch) {
            if (!(prediction.getPredictionData().getData() instanceof Map<?, ?> confidence)
                    || confidence.size() != QUESTION_COUNT) {
                throw new IllegalStateException("Invalid question snapshot for Game 3 prediction " + prediction.getId());
            }
            confidenceByPrediction.put(prediction.getId(), confidence);
            confidence.keySet().forEach(id -> questionIds.add(Long.parseLong(id.toString())));
        }

        Map<Long, Game3QuestionRating> ratings = userRatingRepository.summarizeGame3Ratings(
                confidenceByPrediction.keySet(), questionIds, now).stream()
                .collect(Collectors.toMap(Game3QuestionRating::getQuestionId, rating -> rating));

        List<Prediction> winners = new ArrayList<>();
        for (Prediction prediction : batch) {
            if (evaluate(prediction, confidenceByPrediction.get(prediction.getId()), ratings, now, closingSemester)) {
                winners.add(prediction);
            }
        }
        awardWinners(winners, semester);
        return batch.size();
    }

    private boolean evaluate(Prediction prediction, Map<?, ?> confidence, Map<Long, Game3QuestionRating> ratings,
            Instant now, boolean closingSemester) {
        List<QuestionEvaluation> evaluations = new ArrayList<>(QUESTION_COUNT);
        for (var entry : confidence.entrySet()) {
            long questionId = Long.parseLong(entry.getKey().toString());
            Double declared = entry.getValue() instanceof Number value ? value.doubleValue() : null;
            evaluations.add(evaluateQuestion(questionId, declared, ratings.get(questionId)));
        }
        long matches = evaluations.stream().filter(row -> row.verdict() == QuestionVerdict.MATCH).count();
        long misses = evaluations.stream().filter(row -> row.verdict() == QuestionVerdict.MISMATCH).count();

        Instant finalDue = prediction.getCreatedAt().plus(FINAL_REVIEW_DAYS, ChronoUnit.DAYS);
        boolean won = matches >= MATCHES_TO_WIN;
        boolean lost = misses >= MISSES_TO_LOSE;
        boolean interim = prediction.getTargetDate().isBefore(finalDue)
                && (!closingSemester || prediction.getTargetDate().isBefore(now));
        if (!won && !lost && interim) {
            prediction.setTargetDate(finalDue);
            return false;
        }

        prediction.setStatus(PredictionStatus.RESOLVED);
        prediction.setCorrect(won);
        prediction.setResolvedAt(now);
        // JSONB does not retain the key order of the original confidence map.
        evaluations.sort(Comparator
                .comparing(QuestionEvaluation::displayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(QuestionEvaluation::questionId));
        prediction.setActualData(new PredictionData(Map.of(
                "outcome", won ? "WON" : lost ? "LOST" : "INSUFFICIENT_RATINGS",
                "matchedQuestions", matches,
                "nonMatchingQuestions", misses,
                "questionEvaluations", List.copyOf(evaluations))));
        return won;
    }

    private QuestionEvaluation evaluateQuestion(long questionId, Double confidence, Game3QuestionRating rating) {
        long ratingCount = rating == null ? 0 : rating.getRatingCount();
        Double average = rating == null ? null : rating.getAverageRating();
        QuestionVerdict verdict;
        if (ratingCount < MINIMUM_RATERS || average == null) {
            verdict = QuestionVerdict.INSUFFICIENT_RATINGS;
        } else if (confidence == null) {
            verdict = QuestionVerdict.MISSING_CONFIDENCE;
        } else {
            verdict = Math.abs(confidence - average) <= MAX_RATING_DIFFERENCE
                    ? QuestionVerdict.MATCH : QuestionVerdict.MISMATCH;
        }
        return new QuestionEvaluation(questionId, rating == null ? null : rating.getQuestionCode(),
                rating == null ? null : rating.getDisplayOrder(), confidence, average, ratingCount, verdict);
    }

    private enum QuestionVerdict {
        MATCH, MISMATCH, INSUFFICIENT_RATINGS, MISSING_CONFIDENCE
    }

    private record QuestionEvaluation(long questionId, String questionCode, Integer displayOrder,
            Double declaredConfidence, Double averageRating, long ratingCount, QuestionVerdict verdict) {
    }

    private void awardWinners(List<Prediction> winners, Semester semester) {
        if (winners.isEmpty()) {
            return;
        }
        List<Long> sessionIds = winners.stream().map(Prediction::getTargetId).toList();
        Map<Long, Subject> subjects = sessionRepository.findByIdInFetchSubjects(sessionIds).stream()
                .collect(Collectors.toMap(Session::getId, session -> session.getCourseClass().getSubject()));
        List<UserPointRewardDto> rewards = winners.stream().map(prediction -> new UserPointRewardDto(
                prediction.getGambler().getId(), Point.GAME3_AWARD.getPoints(), 0,
                PointHistoryReason.GAME_3_AUTHOR_CONFIDENCE_WIN, PointHistoryTargetType.SESSION,
                prediction.getTargetId(), subjects.get(prediction.getTargetId()))).toList();
        counterMetricsService.awardGamePointsBatch(rewards, semester);
    }
}
