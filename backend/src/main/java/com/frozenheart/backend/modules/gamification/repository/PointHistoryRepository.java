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
        GROUP BY  p.user.id
    """)
    List<LeaderboardAggregation> getAggregatedPointsForRebuild(
            @Param("subjectId") Long subjectId,
            @Param("semesterId") Long semesterId
    );

}
