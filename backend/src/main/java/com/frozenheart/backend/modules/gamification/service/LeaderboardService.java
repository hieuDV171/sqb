package com.frozenheart.backend.modules.gamification.service;

import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LeaderboardService {

    void updateLeaderboard(Long userId, double addedPoints, List<String> leaderboardKeys);

    @Transactional
    void rebuildLeaderboard(LeaderboardPeriod period, Long subjectId);

    void rebuildLeaderboard(Long subjectId, Long semesterId, String leaderboardKey);

    void scheduleOldSemesterCleanup(Long oldSemesterId);
}
