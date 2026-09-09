package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.PointHistory;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    @Query("""
        SELECT p.user.id AS userId, SUM(p.points) AS totalPoints, MAX(p.createdAt) AS lastEventTime
        FROM PointHistory p
        WHERE (:semesterId IS NULL OR p.semester.id = :semesterId)
            AND (:subjectId IS NULL OR p.subject.id = :subjectId)
            AND ((p.semester IS NOT NULL AND p.semester.isFinalized = TRUE)
                 OR p.reason IS NULL
                 OR p.reason NOT IN (com.frozenheart.backend.core.entity.prediction.PointHistoryReason.GAME_5_REPORT_ERROR_APPROVED, com.frozenheart.backend.core.entity.prediction.PointHistoryReason.SECRET_POINTS_AWARDED))
        GROUP BY p.user.id
    """)
    List<LeaderboardAggregation> getAggregatedPointsForRebuild(
            @Param("subjectId") Long subjectId,
            @Param("semesterId") Long semesterId
    );

    @Query("""
        SELECT p FROM PointHistory p
        LEFT JOIN FETCH p.user
        WHERE p.semester.id = :semesterId
          AND p.reason = com.frozenheart.backend.core.entity.prediction.PointHistoryReason.GAME_5_REPORT_ERROR_APPROVED
    """)
    List<PointHistory> findApprovedErrorHistoriesInSemester(@Param("semesterId") Long semesterId);

}
