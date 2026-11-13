package com.frozenheart.backend.modules.activityfeed.repository;

import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {

    String CURRENT_BADGE_AWARD_FILTER = """
            AND (f.actionType IS NULL OR f.actionType <> 'EARNED_BADGE' OR EXISTS (
                SELECT 1 FROM UserBadge ub WHERE ub.id.badgeId = f.targetId AND ub.id.userId = f.user.id
                  AND ub.revokedAt IS NULL AND ub.earnedAt <= f.createdAt
            ))
            """;

    String LECTURE_VIDEO_FILTER = """
            AND (f.actionType IS NULL OR f.actionType <> 'PUBLISHED_LECTURE_VIDEO' OR EXISTS (
                SELECT 1 FROM Post p
                WHERE f.targetType = 'POST' AND p.id = f.targetId
                  AND p.postType = 'LEARNING_VIDEO' AND p.deletedAt IS NULL AND
            """ + PostRepository.LECTURE_VIDEO_ACCESS + "AND " + PostRepository.POST_VISIBILITY_ACCESS + "))\n";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT f FROM ActivityFeed f JOIN FETCH f.user
            WHERE f.id = :feedId
              AND f.actionType = com.frozenheart.backend.core.entity.activityfeed.ActionType.EARNED_BADGE
            """ + CURRENT_BADGE_AWARD_FILTER)
    Optional<ActivityFeed> findBadgeAwardForUpdate(@Param("feedId") Long feedId);

    @EntityGraph(attributePaths = { "user" })
    @Query("""
            SELECT f FROM ActivityFeed f WHERE f.id < :cursor
              AND (f.actionType IS NULL OR f.actionType <> 'PUBLISHED_LECTURE_VIDEO')
            """ + CURRENT_BADGE_AWARD_FILTER + """
            ORDER BY f.id DESC
            """)
    List<ActivityFeed> findAnonymousFeeds(@Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    @Query("""
            SELECT f
            FROM ActivityFeed f
            WHERE f.id < :cursor
              AND (:hasBlocked = false OR f.user.id NOT IN :blockedUserIds)
              AND (
                  f.user.id IN :networkUserIds
                  OR f.actionType IN (
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.SESSION_RESOLVED_APPROVED,
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.PUBLISHED_LECTURE_VIDEO,
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.EARNED_BADGE,
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.REACHED_MILESTONE
                  )
              )
            """ + LECTURE_VIDEO_FILTER + CURRENT_BADGE_AWARD_FILTER + """
            ORDER BY f.id DESC
            """)
    List<ActivityFeed> findPersonalizedFeeds(
            @Param("cursor") Long cursor,
            @Param("networkUserIds") Collection<Long> networkUserIds,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("hasBlocked") boolean hasBlocked,
            @Param("viewerId") Long viewerId,
            @Param("admin") boolean admin,
            Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    @Query("""
            SELECT f
            FROM ActivityFeed f
            WHERE f.id < :cursor
              AND (:hasBlocked = false OR f.user.id NOT IN :blockedUserIds)
            """ + CURRENT_BADGE_AWARD_FILTER + """
            ORDER BY f.id DESC
            """)
    List<ActivityFeed> findPublicFeedsExcludingBlocked(
            @Param("cursor") Long cursor,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("hasBlocked") boolean hasBlocked,
            Pageable pageable);

    @Query("""
            SELECT COUNT(f) FROM ActivityFeed f WHERE f.createdAt > :since
              AND (f.actionType IS NULL OR f.actionType <> 'PUBLISHED_LECTURE_VIDEO')
            """ + CURRENT_BADGE_AWARD_FILTER)
    int countAnonymousNewFeeds(@Param("since") Instant since);

    @Query("""
            SELECT COUNT(f)
            FROM ActivityFeed f
            WHERE f.createdAt > :since
              AND (:hasBlocked = false OR f.user.id NOT IN :blockedUserIds)
              AND (
                  f.user.id IN :networkUserIds
                  OR f.actionType IN (
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.SESSION_RESOLVED_APPROVED,
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.PUBLISHED_LECTURE_VIDEO,
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.EARNED_BADGE,
                      com.frozenheart.backend.core.entity.activityfeed.ActionType.REACHED_MILESTONE
                  )
              )
            """ + LECTURE_VIDEO_FILTER + CURRENT_BADGE_AWARD_FILTER)
    int countPersonalizedNewFeeds(
            @Param("since") Instant since,
            @Param("networkUserIds") Collection<Long> networkUserIds,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("hasBlocked") boolean hasBlocked,
            @Param("viewerId") Long viewerId,
            @Param("admin") boolean admin);

    int countByIdGreaterThan(Long afterId);
}
