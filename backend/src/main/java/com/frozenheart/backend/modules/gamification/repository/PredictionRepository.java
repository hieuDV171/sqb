package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.prediction.GameType;
import com.frozenheart.backend.core.entity.prediction.Prediction;
import com.frozenheart.backend.core.entity.prediction.PredictionStatus;
import com.frozenheart.backend.core.entity.prediction.PredictionTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Query("""
            SELECT p FROM Prediction p JOIN FETCH p.gambler
            WHERE p.gameType = :gameType AND p.targetType = :targetType
              AND p.targetId IN :targetIds AND p.status = :status
            """)
    List<Prediction> findPendingForTargets(@Param("gameType") GameType gameType,
            @Param("targetType") PredictionTargetType targetType, @Param("targetIds") Set<Long> targetIds,
            @Param("status") PredictionStatus status);

    boolean existsByGamblerIdAndGameTypeAndTargetId(Long gamblerId, GameType gameType, Long targetId);

    boolean existsByGamblerIdAndGameTypeAndTargetTypeAndTargetIdAndTargetDateBetween(
            Long gamblerId, GameType gameType, PredictionTargetType targetType, Long targetId, Instant start, Instant end);

    List<Prediction> findByGamblerIdAndGameTypeAndTargetTypeAndTargetDateBetween(
            Long gamblerId, GameType gameType, PredictionTargetType targetType, Instant start, Instant end);

    @Query("SELECT MIN(p.targetDate) FROM Prediction p WHERE p.gameType = :gameType AND p.status = :status AND p.targetDate < :cutoff")
    Optional<Instant> findOldestTargetDateBefore(@Param("gameType") GameType gameType,
            @Param("status") PredictionStatus status, @Param("cutoff") Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Prediction p JOIN FETCH p.gambler
            WHERE p.gameType = :gameType AND p.status = :status AND p.targetDate >= :start AND p.targetDate < :end
            ORDER BY p.targetDate, p.id
            """)
    List<Prediction> findForResolution(@Param("gameType") GameType gameType,
            @Param("status") PredictionStatus status, @Param("start") Instant start, @Param("end") Instant end,
            Pageable pageable);

    Optional<Prediction> findByGamblerIdAndGameTypeAndTargetId(Long gamblerId, GameType gameType, Long targetId);

    @Query("""
            SELECT p FROM Prediction p JOIN FETCH p.gambler
            WHERE p.gameType = com.frozenheart.backend.core.entity.prediction.GameType.GAME_3_AUTHOR_CONFIDENCE
              AND p.targetType = com.frozenheart.backend.core.entity.prediction.PredictionTargetType.SESSION
              AND p.status = com.frozenheart.backend.core.entity.prediction.PredictionStatus.PENDING
              AND (:closingSemester = true OR p.targetDate <= :now)
              AND EXISTS (SELECT s.id FROM Session s WHERE s.id = p.targetId AND s.courseClass.semester.id = :semesterId)
            ORDER BY p.targetDate, p.id
            """)
    List<Prediction> findPendingGame3Batch(@Param("semesterId") Long semesterId,
            @Param("now") Instant now, @Param("closingSemester") boolean closingSemester, Pageable pageable);

}
