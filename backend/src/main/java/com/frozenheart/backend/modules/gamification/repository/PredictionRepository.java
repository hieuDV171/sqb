package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByGamblerId(Long gamblerId);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.gambler WHERE p.gameType = :gameType AND p.status = :status")
    List<Prediction> findByGameTypeAndStatusFetchGambler(@Param("gameType") GameType gameType, @Param("status") PredictionStatus status);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.gambler WHERE p.gameType = :gameType AND p.targetType = :targetType AND p.targetId = :targetId")
    List<Prediction> findByGameTypeAndTargetTypeAndTargetIdFetchGambler(@Param("gameType") GameType gameType, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    boolean existsByGamblerIdAndGameTypeAndTargetId(Long gamblerId, GameType gameType, Long targetId);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.gambler WHERE p.gameType = :gameType AND p.targetDate >= :start AND p.targetDate < :end")
    List<Prediction> findByGameTypeAndTargetDateBetweenFetchGambler(@Param("gameType") GameType gameType, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<Prediction> findByGamblerIdAndGameTypeAndTargetId(Long gamblerId, GameType gameType, Long targetId);

}
