package com.frozenheart.backend.modules.badge.repository;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByName(String name);

    boolean existsByName(String name);

    List<Badge> findByActiveTrueOrderByIdAsc();

    List<Badge> findByActiveTrueAndBadgeTriggerEvent(BadgeTriggerEvent badgeTriggerEvent);

    @Query("""
        SELECT b
        FROM Badge b
        WHERE b.active = true
          AND b.id NOT IN (
            SELECT ub.id.badgeId
            FROM UserBadge ub
            WHERE ub.id.userId = :userId
        )
        ORDER BY b.id ASC
        """)
    List<Badge> findUnearnedActiveBadgesByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT b
        FROM Badge b
        WHERE b.active = true
          AND (:badgeTriggerEvent IS NULL OR b.badgeTriggerEvent = :badgeTriggerEvent)
          AND b.id NOT IN (
            SELECT ub.id.badgeId
            FROM UserBadge ub
            WHERE ub.id.userId = :userId
        )
        ORDER BY b.id ASC
        """)
    List<Badge> findUnearnedActiveBadgesByUserAndBadgeTriggerEvent(
            @Param("userId") Long userId,
            @Param("badgeTriggerEvent") BadgeTriggerEvent badgeTriggerEvent);
}
