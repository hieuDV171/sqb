package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserFollow;
import com.frozenheart.backend.core.entity.socialinteraction.UserFollowId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    Optional<UserFollow> findByFollowerIdAndFollowedUserId(Long followerId, Long followedUserId);

    Boolean existsByFollowerIdAndFollowedUserId(Long followerId, Long followedUserId);

    int countByFollowerId(Long followerId);

    int countByFollowedUserId(Long followedUserId);

    @Query("""
        SELECT f.follower.id
        FROM UserFollow f
        WHERE f.followedUser.id = :currentUserId
          AND f.follower.id IN :targetUserIds
        """)
    Set<Long> findFollowerIdsFollowingUser(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserIds") Collection<Long> targetUserIds);

    @Query("""
        SELECT f.followedUser.id
        FROM UserFollow f
        WHERE f.follower.id = :currentUserId
          AND f.followedUser.id IN :targetUserIds
        """)
    Set<Long> findFollowedIdsFollowedByUser(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserIds") Collection<Long> targetUserIds);

    @EntityGraph(attributePaths = {"followedUser"})
    @Query("""
        SELECT f
        FROM UserFollow f
        WHERE f.follower.id = :followerId
          AND f.createdAt < :cursor
        ORDER BY f.createdAt DESC
        """)
    List<UserFollow> findFollowingCursor(
            @Param("followerId") Long followerId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    @EntityGraph(attributePaths = {"follower"})
    @Query("""
        SELECT f
        FROM UserFollow f
        WHERE f.followedUser.id = :followedUserId
          AND f.createdAt < :cursor
        ORDER BY f.createdAt DESC
        """)
    List<UserFollow> findFollowersCursor(
            @Param("followedUserId") Long followedUserId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);
}
