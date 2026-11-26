package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.user.UserGamification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {

    Optional<UserGamification> findByUserId(Long userId);

    List<UserGamification> findAllBySecretPointsGreaterThan(double points);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM UserGamification g WHERE g.userId = :userId")
    Optional<UserGamification> findWalletForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserGamification g SET g.publicPoints = g.publicPoints + :delta WHERE g.userId = :userId")
    int incrementPublicPoints(@Param("userId") Long userId, @Param("delta") double delta);

    @Modifying
    @Query("UPDATE UserGamification g SET g.secretPoints = g.secretPoints + :delta WHERE g.userId = :userId")
    int incrementSecretPoints(@Param("userId") Long userId, @Param("delta") double delta);
}
