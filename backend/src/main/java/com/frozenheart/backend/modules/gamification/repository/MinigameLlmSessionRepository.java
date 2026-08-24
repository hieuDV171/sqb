package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.MinigameLlmSession;
import com.frozenheart.backend.core.entity.prediction.MinigameLlmSessionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MinigameLlmSessionRepository extends JpaRepository<MinigameLlmSession, Long> {

    Optional<MinigameLlmSession> findFirstByStatusOrderByStartTimeDesc(MinigameLlmSessionStatus status);

    Optional<MinigameLlmSession> findByWeekNumberAndYear(int weekNumber, int year);

}
