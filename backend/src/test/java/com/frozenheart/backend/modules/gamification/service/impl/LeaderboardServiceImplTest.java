package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.dto.event.PointAddedEvent;
import com.frozenheart.backend.core.util.RedisKeyUtil;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
import com.frozenheart.backend.modules.gamification.repository.PointHistoryRepository;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private CurrentSemesterHolder currentSemesterHolder;
    @Mock
    private PointHistoryRepository pointHistoryRepository;
    private LeaderboardServiceImpl leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = new LeaderboardServiceImpl(
                redisTemplate,
                currentSemesterHolder,
                pointHistoryRepository);
    }

    @Test
    void gamePointsUpdateTheSemesterRecordedWithTheAward() {
        Long awardSemester = 12L;
        Long subjectId = 7L;
        List<String> keys = List.of(
                RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.SEMESTER, awardSemester, null),
                RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.SUBJECT, awardSemester, subjectId));

        leaderboardService.handlePointAddedEvent(new PointAddedEvent(25L, 0.25, subjectId, awardSemester));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(keys), eq("25"), eq("25"));
        verifyNoInteractions(currentSemesterHolder);
    }

    @Test
    void sendsPlainNumericStringsToLeaderboardLuaScript() {
        List<String> keys = List.of("leaderboard:semester:20261");

        leaderboardService.updateLeaderboard(25L, 0.5, keys);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(keys), eq("25"), eq("50"));
        assertEquals(Long.class, scriptCaptor.getValue().getResultType());
        assertTrue(scriptCaptor.getValue().getScriptAsString().contains("tonumber(ARGV[2])"));
    }

    @Test
    void readsPostgresBeforeReplacingExistingRedisLeaderboard() {
        String leaderboardKey = "leaderboard:semester:20261";
        when(pointHistoryRepository.getAggregatedPointsForRebuild(null, 20261L))
                .thenReturn(List.of());

        leaderboardService.rebuildLeaderboard(null, 20261L, leaderboardKey);

        var inOrder = org.mockito.Mockito.inOrder(pointHistoryRepository, redisTemplate);
        inOrder.verify(pointHistoryRepository).getAggregatedPointsForRebuild(null, 20261L);
        inOrder.verify(redisTemplate).delete(leaderboardKey);
        verify(redisTemplate, org.mockito.Mockito.never()).opsForZSet();
    }
}
