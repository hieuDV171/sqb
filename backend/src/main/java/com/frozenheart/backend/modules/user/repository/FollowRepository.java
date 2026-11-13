package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserFollow;
import com.frozenheart.backend.core.entity.socialinteraction.UserFollowId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

  @Query("""
      SELECT f.followedUser.id
      FROM UserFollow f
      WHERE f.follower.id = :currentUserId
      """)
  Set<Long> findAllFollowedUserIds(@Param("currentUserId") Long currentUserId);

  @EntityGraph(attributePaths = { "followedUser" })
  @Query("""
      SELECT f
      FROM UserFollow f
      WHERE f.follower.id = :followerId
        AND (:firstPage = true
            OR (:withoutTimestamp = false AND (f.createdAt < :cursor OR f.createdAt IS NULL OR (f.createdAt = :cursor AND f.followedUser.id < :cursorId)))
            OR (:withoutTimestamp = true AND f.createdAt IS NULL AND f.followedUser.id < :cursorId))
      ORDER BY f.createdAt DESC NULLS LAST, f.followedUser.id DESC
      """)
  List<UserFollow> findFollowingCursor(
      @Param("followerId") Long followerId,
      @Param("cursor") Instant cursor,
      @Param("cursorId") Long cursorId, @Param("firstPage") boolean firstPage, @Param("withoutTimestamp") boolean withoutTimestamp,
      Pageable pageable);

  @EntityGraph(attributePaths = { "follower" })
  @Query("""
      SELECT f
      FROM UserFollow f
      WHERE f.followedUser.id = :followedUserId
        AND (:firstPage = true
            OR (:withoutTimestamp = false AND (f.createdAt < :cursor OR f.createdAt IS NULL OR (f.createdAt = :cursor AND f.follower.id < :cursorId)))
            OR (:withoutTimestamp = true AND f.createdAt IS NULL AND f.follower.id < :cursorId))
      ORDER BY f.createdAt DESC NULLS LAST, f.follower.id DESC
      """)
  List<UserFollow> findFollowersCursor(
      @Param("followedUserId") Long followedUserId,
      @Param("cursor") Instant cursor,
      @Param("cursorId") Long cursorId, @Param("firstPage") boolean firstPage, @Param("withoutTimestamp") boolean withoutTimestamp,
      Pageable pageable);
}
