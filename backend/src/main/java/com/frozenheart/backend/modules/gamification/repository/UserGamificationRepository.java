package com.frozenheart.backend.modules.gamification.repository;

import com.frozenheart.backend.core.entity.user.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {

    Optional<UserGamification> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserGamification g SET g.coinBalance = g.coinBalance - :amount WHERE g.userId = :userId AND g.coinBalance >= :amount")
    int deductCoinBalance(@Param("userId") Long userId, @Param("amount") double amount);

    @Modifying
    @Query("UPDATE UserGamification g SET g.coinBalance = g.coinBalance + :amount WHERE g.userId = :userId")
    int addCoinBalance(@Param("userId") Long userId, @Param("amount") double amount);

    @Modifying
    @Query("UPDATE UserGamification g SET g.publicPoints = g.publicPoints + :delta WHERE g.userId = :userId")
    int incrementPublicPoints(@Param("userId") Long userId, @Param("delta") double delta);

    @Modifying
    @Query("UPDATE UserGamification g SET g.secretPoints = g.secretPoints + :delta WHERE g.userId = :userId")
    int incrementSecretPoints(@Param("userId") Long userId, @Param("delta") double delta);
}
