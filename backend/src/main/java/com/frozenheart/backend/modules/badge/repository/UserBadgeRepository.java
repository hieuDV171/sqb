package com.frozenheart.backend.modules.badge.repository;

import com.frozenheart.backend.core.entity.badge.UserBadge;
import com.frozenheart.backend.core.entity.badge.UserBadgeId;
import com.frozenheart.backend.modules.badge.dto.BadgeAccountResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UserBadgeId> {

    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.id.userId = :userId AND ub.revokedAt IS NULL ORDER BY ub.earnedAt DESC")
    List<UserBadge> findCurrentByUserId(@Param("userId") Long userId);

    boolean existsByIdUserIdAndIdBadgeId(Long userId, Long badgeId);

    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.id.badgeId = :badgeId AND ub.revokedAt IS NULL")
    int countByBadgeId(@Param("badgeId") Long badgeId);

    interface BadgeEarnedCount {
        Long getBadgeId();
        Long getEarnedCount();
    }

    @Query("SELECT ub.id.badgeId AS badgeId, COUNT(ub) AS earnedCount FROM UserBadge ub WHERE ub.id.badgeId IN :badgeIds AND ub.revokedAt IS NULL GROUP BY ub.id.badgeId")
    List<BadgeEarnedCount> countGroupedByBadgeId(@Param("badgeIds") Collection<Long> badgeIds);

    @Modifying
    @Query("UPDATE UserBadge ub SET ub.revokedAt = :now WHERE ub.id.badgeId = :badgeId AND ub.id.userId IN :userIds AND ub.revokedAt IS NULL")
    int revokeAwards(@Param("badgeId") Long badgeId, @Param("userIds") Collection<Long> userIds, @Param("now") Instant now);

    @Query("SELECT ub.id.userId FROM UserBadge ub WHERE ub.id.badgeId = :badgeId AND ub.id.userId IN :userIds AND ub.revokedAt IS NULL")
    Set<Long> findCurrentUserIds(@Param("badgeId") Long badgeId, @Param("userIds") Collection<Long> userIds);

    @Modifying
    @Query(value = """
            INSERT INTO user_badges (user_id, badge_id, earned_at)
            SELECT :userId, b.id, :now FROM badges b WHERE b.id = :badgeId AND b.active = true
            ON CONFLICT (user_id, badge_id) DO NOTHING
            """, nativeQuery = true)
    int insertAutomaticAward(@Param("userId") Long userId, @Param("badgeId") Long badgeId, @Param("now") Instant now);

    @Query(value = """
            WITH awarded AS (
                INSERT INTO user_badges (user_id, badge_id, earned_at)
                SELECT u.id, :badgeId, :now FROM users u WHERE u.id IN (:userIds) ORDER BY u.id
                ON CONFLICT (user_id, badge_id) DO UPDATE
                    SET earned_at = EXCLUDED.earned_at, revoked_at = NULL
                    WHERE user_badges.revoked_at IS NOT NULL
                RETURNING user_id
            ) SELECT user_id FROM awarded
            """, nativeQuery = true)
    List<Long> grantAwards(@Param("badgeId") Long badgeId, @Param("userIds") Collection<Long> userIds, @Param("now") Instant now);

    @Query("""
            SELECT new com.frozenheart.backend.modules.badge.dto.BadgeAccountResponse(
                u.id, p.fullName, CASE WHEN u.active = true THEN u.email ELSE null END, u.role)
            FROM User u JOIN UserProfile p ON p.userId = u.id
            LEFT JOIN UserBadge ub ON ub.id.userId = u.id AND ub.id.badgeId = :badgeId AND ub.revokedAt IS NULL
            WHERE u.id > :after
              AND ((:holders = true AND ub.id.userId IS NOT NULL)
                OR (:holders = false AND u.active = true AND ub.id.userId IS NULL))
              AND (:query = '' OR locate(:query, lower(p.fullName)) > 0
                OR (u.active = true AND locate(:query, lower(u.email)) > 0))
            ORDER BY u.id
            """)
    List<BadgeAccountResponse> searchAccounts(@Param("badgeId") Long badgeId, @Param("holders") boolean holders,
            @Param("query") String query, @Param("after") Long after, Pageable pageable);

    @Query("SELECT ub FROM UserBadge ub WHERE ub.id.userId IN :userIds AND ub.id.badgeId IN :badgeIds")
    List<UserBadge> findByUserIdsAndBadgeIds(@Param("userIds") Collection<Long> userIds, @Param("badgeIds") Collection<Long> badgeIds);
}
