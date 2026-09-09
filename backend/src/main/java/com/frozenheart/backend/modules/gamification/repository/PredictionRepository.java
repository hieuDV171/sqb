package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.prediction.PredictionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByGamblerId(Long gamblerId);

    @Query("SELECT p FROM Prediction p WHERE p.gambler.id = :gamblerId AND (:after IS NULL OR p.id < :after) ORDER BY p.id DESC")
    List<Prediction> findByGamblerIdWithCursor(@Param("gamblerId") Long gamblerId, @Param("after") Long after,
            Pageable pageable);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.gambler WHERE p.gameType = :gameType AND p.status = :status")
    List<Prediction> findByGameTypeAndStatusFetchGambler(@Param("gameType") GameType gameType,
            @Param("status") PredictionStatus status);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.gambler WHERE p.gameType = :gameType AND p.targetType = :targetType AND p.targetId = :targetId")
    List<Prediction> findByGameTypeAndTargetTypeAndTargetIdFetchGambler(@Param("gameType") GameType gameType,
            @Param("targetType") PredictionTargetType targetType, @Param("targetId") Long targetId);

    boolean existsByGamblerIdAndGameTypeAndTargetId(Long gamblerId, GameType gameType, Long targetId);

    boolean existsByGamblerIdAndGameTypeAndTargetTypeAndTargetIdAndTargetDateBetween(
            Long gamblerId, GameType gameType, PredictionTargetType targetType, Long targetId, Instant start, Instant end);

    List<Prediction> findByGamblerIdAndGameTypeAndTargetTypeAndTargetDateBetween(
            Long gamblerId, GameType gameType, PredictionTargetType targetType, Instant start, Instant end);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.gambler WHERE p.gameType = :gameType AND p.targetDate >= :start AND p.targetDate < :end")
    List<Prediction> findByGameTypeAndTargetDateBetweenFetchGambler(@Param("gameType") GameType gameType,
            @Param("start") Instant start, @Param("end") Instant end);

    Optional<Prediction> findByGamblerIdAndGameTypeAndTargetId(Long gamblerId, GameType gameType, Long targetId);

}
