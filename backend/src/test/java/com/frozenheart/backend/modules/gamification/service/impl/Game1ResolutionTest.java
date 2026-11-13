package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.PointHistoryReason;
import com.frozenheart.backend.core.entity.prediction.PointHistoryTargetType;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.prediction.PredictionData;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.gamification.repository.PredictionRepository;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import com.frozenheart.backend.modules.user.dto.UserPointRewardDto;
import com.frozenheart.backend.modules.session.service.CurrentSemesterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Game1ResolutionTest {
    private static final ZoneId SCHOOL_ZONE = ZoneId.of(Time.DEFAULT_TIMEZONE);

    @Mock private PredictionRepository predictionRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private CounterMetricsService counterMetricsService;
    @Mock private CourseClassRepository courseClassRepository;
    @Mock private CurrentSemesterService currentSemesterService;
    @Mock private StringRedisTemplate redisTemplate;
    @InjectMocks private GamificationServiceImpl service;

    @Test
    void resolvesMissedTargetDayUsingItsOwnParticipantCounts() {
        var today = LocalDate.now(SCHOOL_ZONE);
        var missedDay = today.minusDays(3);
        var cutoff = today.atStartOfDay(SCHOOL_ZONE).toInstant();
        var start = missedDay.atStartOfDay(SCHOOL_ZONE).toInstant();
        var end = missedDay.plusDays(1).atStartOfDay(SCHOOL_ZONE).toInstant();
        Prediction win = prediction(42L, 2);
        Prediction loss = prediction(43L, 2);
        when(predictionRepository.findOldestTargetDateBefore(GameType.GAME_1_PARTICIPANTS, PredictionStatus.PENDING, cutoff))
                .thenReturn(Optional.of(start));
        when(predictionRepository.findForResolution(GameType.GAME_1_PARTICIPANTS, PredictionStatus.PENDING, start, end, PageRequest.of(0, 500)))
                .thenReturn(List.of(win, loss));
        when(sessionRepository.countDistinctProposersByCourseClassesBetween(Set.of(42L, 43L), start, end))
                .thenReturn(List.<Object[]>of(new Object[] {42L, 3L}));
        Semester originalSemester = Semester.builder().id(12L).build();
        when(courseClassRepository.findByIdInFetchSemester(Set.of(42L)))
                .thenReturn(List.of(CourseClass.builder().id(42L).semester(originalSemester).build()));

        assertEquals(2, service.resolveNextGame1Batch());

        assertEquals(PredictionStatus.RESOLVED, win.getStatus());
        assertEquals(PredictionStatus.RESOLVED, loss.getStatus());
        assertEquals(Map.of("actualCount", 3L, "courseClassId", 42L), win.getActualData().getData());
        assertEquals(Map.of("actualCount", 0L, "courseClassId", 43L), loss.getActualData().getData());
        assertTrue(win.isCorrect());
        assertFalse(loss.isCorrect());
        verify(counterMetricsService).awardGamePointsBatch(List.of(new UserPointRewardDto(
                7L, Point.GAME1_AWARD.getPoints(), 0, PointHistoryReason.GAME_1_WIN,
                PointHistoryTargetType.COURSE_CLASS, 42L, null)), originalSemester);
        verifyNoInteractions(currentSemesterService);
        verifyNoMoreInteractions(counterMetricsService);
    }

    @Test
    void leavesCurrentDayForAFutureRun() {
        var cutoff = LocalDate.now(SCHOOL_ZONE).atStartOfDay(SCHOOL_ZONE).toInstant();
        when(predictionRepository.findOldestTargetDateBefore(GameType.GAME_1_PARTICIPANTS, PredictionStatus.PENDING, cutoff))
                .thenReturn(Optional.empty());

        assertEquals(0, service.resolveNextGame1Batch());

        verify(predictionRepository).findOldestTargetDateBefore(GameType.GAME_1_PARTICIPANTS, PredictionStatus.PENDING, cutoff);
        verifyNoMoreInteractions(predictionRepository);
        verifyNoInteractions(sessionRepository, counterMetricsService);
    }

    @Test
    void leaderboardIsEmptyBetweenSemesters() {
        var authentication = new UsernamePasswordAuthenticationToken("7", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(7L).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var result = service.getLeaderboard(LeaderboardPeriod.SEMESTER, null, null, null, 20);
            assertTrue(result.entries().isEmpty());
            assertEquals(0, result.myRank().rank());
            assertFalse(result.pagination().hasNext());
            verifyNoInteractions(redisTemplate);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Prediction prediction(Long classId, int guessedCount) {
        var prediction = new Prediction();
        prediction.setGambler(User.builder().id(7L).build());
        prediction.setTargetId(classId);
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setPredictionData(PredictionData.builder().data(Map.of("predictedCount", guessedCount)).build());
        return prediction;
    }
}
