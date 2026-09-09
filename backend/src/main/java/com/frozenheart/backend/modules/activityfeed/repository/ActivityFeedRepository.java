package com.frozenheart.backend.modules.activityfeed.repository;

import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {

    @EntityGraph(attributePaths = { "user" })
    List<ActivityFeed> findByIdLessThanOrderByIdDesc(Long after, Pageable pageable);

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
            ORDER BY f.id DESC
            """)
    List<ActivityFeed> findPersonalizedFeeds(
            @Param("cursor") Long cursor,
            @Param("networkUserIds") Collection<Long> networkUserIds,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("hasBlocked") boolean hasBlocked,
            Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    @Query("""
            SELECT f
            FROM ActivityFeed f
            WHERE f.id < :cursor
              AND (:hasBlocked = false OR f.user.id NOT IN :blockedUserIds)
            ORDER BY f.id DESC
            """)
    List<ActivityFeed> findPublicFeedsExcludingBlocked(
            @Param("cursor") Long cursor,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("hasBlocked") boolean hasBlocked,
            Pageable pageable);

    int countByCreatedAtAfter(Instant since);

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
            """)
    int countPersonalizedNewFeeds(
            @Param("since") Instant since,
            @Param("networkUserIds") Collection<Long> networkUserIds,
            @Param("blockedUserIds") Collection<Long> blockedUserIds,
            @Param("hasBlocked") boolean hasBlocked);

    int countByIdGreaterThan(Long afterId);
}
