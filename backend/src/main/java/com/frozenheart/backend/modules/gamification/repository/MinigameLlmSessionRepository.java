package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.MinigameLlmSession;
import com.frozenheart.backend.core.entity.prediction.MinigameLlmSessionStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MinigameLlmSessionRepository extends JpaRepository<MinigameLlmSession, Long> {

    @Query("""
            SELECT DISTINCT gs
            FROM MinigameLlmSession gs
            LEFT JOIN FETCH gs.questions
            WHERE gs.status = :status
            ORDER BY gs.startTime DESC
            """)
    Optional<MinigameLlmSession> findByStatusFetchQuestions(@Param("status") MinigameLlmSessionStatus status);

    @EntityGraph(attributePaths = {"questions"})
    Optional<MinigameLlmSession> findFirstByStatusOrderByStartTimeDesc(MinigameLlmSessionStatus status);

    Optional<MinigameLlmSession> findByWeekNumberAndYear(int weekNumber, int year);
}
