package com.frozenheart.backend.modules.session.repository;

import com.frozenheart.backend.core.entity.prediction.PointHistory;
import com.frozenheart.backend.core.entity.session.Semester;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    @Query("SELECT ph.user.id, SUM(ph.points) " +
           "FROM PointHistory ph " +
           "WHERE (:subjectId IS NULL OR ph.subject.id = :subjectId) " +
           "AND (:fromDate IS NULL OR ph.createdAt >= :fromDate) " +
           "AND (:semester IS NULL OR ph.semester = :semester) " +
           "GROUP BY ph.user.id " +
           "ORDER BY SUM(ph.points) DESC")
    List<Object[]> findLeaderboardRaw(
            @Param("subjectId") Long subjectId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("semester") Semester semester,
            Pageable pageable
    );

}
