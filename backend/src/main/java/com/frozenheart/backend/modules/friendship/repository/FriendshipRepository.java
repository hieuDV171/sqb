package com.frozenheart.backend.modules.friendship.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipId;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

  @Query("""
      SELECT f
      FROM Friendship f
      LEFT JOIN FETCH f.sender
      LEFT JOIN FETCH f.receiver
      WHERE (f.sender.id = :userId1 AND f.receiver.id = :userId2)
         OR (f.sender.id = :userId2 AND f.receiver.id = :userId1)
      """)
  Optional<Friendship> findFriendshipsBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT f
      FROM Friendship f
      JOIN FETCH f.sender
      JOIN FETCH f.receiver
      WHERE (f.id.senderId = :userId1 AND f.id.receiverId = :userId2)
         OR (f.id.senderId = :userId2 AND f.id.receiverId = :userId1)
      """)
  Optional<Friendship> findFriendshipsBetweenForUpdate(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

  int countByReceiverIdAndStatus(Long receiverId, FriendshipStatus status);

  int countBySenderIdAndStatus(Long senderId, FriendshipStatus status);

  @Query("""
      SELECT COUNT(f)
      FROM Friendship f
      WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
        AND f.status = 'ACCEPTED'
      """)
  int countTotalFriends(@Param("userId") Long userId);

  @Query("""
      SELECT CASE WHEN f.sender.id = :currentUserId THEN f.receiver.id ELSE f.sender.id END
      FROM Friendship f
      WHERE ((f.sender.id = :currentUserId AND f.receiver.id IN :targetUserIds)
          OR (f.receiver.id = :currentUserId AND f.sender.id IN :targetUserIds))
        AND f.status = 'ACCEPTED'
      """)
  Set<Long> findAcceptedFriendUserIds(
      @Param("currentUserId") Long currentUserId,
      @Param("targetUserIds") Collection<Long> targetUserIds);

  @Query("""
      SELECT CASE WHEN f.sender.id = :userId THEN f.receiver.id ELSE f.sender.id END
      FROM Friendship f
      WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
        AND f.status = 'ACCEPTED'
      """)
  Set<Long> findAllAcceptedFriendUserIds(@Param("userId") Long userId);

  @EntityGraph(attributePaths = { "sender", "receiver" })
  @Query("""
      SELECT f
      FROM Friendship f
      WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
        AND f.status = 'ACCEPTED'
      ORDER BY f.acceptedAt DESC
      """)
  List<Friendship> findAcceptedFriendsSimple(@Param("userId") Long userId, Pageable pageable);

  @EntityGraph(attributePaths = { "sender", "receiver" })
  @Query("""
      SELECT f
      FROM Friendship f
      WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
        AND f.status = 'ACCEPTED'
        AND (:firstPage = true
            OR (:withoutTimestamp = false AND (f.acceptedAt < :cursor OR f.acceptedAt IS NULL OR (f.acceptedAt = :cursor AND CASE WHEN f.id.senderId = :userId THEN f.id.receiverId ELSE f.id.senderId END < :cursorId)))
            OR (:withoutTimestamp = true AND f.acceptedAt IS NULL AND CASE WHEN f.id.senderId = :userId THEN f.id.receiverId ELSE f.id.senderId END < :cursorId))
      ORDER BY f.acceptedAt DESC NULLS LAST, CASE WHEN f.id.senderId = :userId THEN f.id.receiverId ELSE f.id.senderId END DESC
      """)
  List<Friendship> findAcceptedFriendsCursor(@Param("userId") Long userId, @Param("cursor") Instant cursor,
      @Param("cursorId") Long cursorId, @Param("firstPage") boolean firstPage, @Param("withoutTimestamp") boolean withoutTimestamp,
      Pageable pageable);

  @EntityGraph(attributePaths = { "sender" })
  @Query("""
      SELECT f
      FROM Friendship f
      WHERE f.receiver.id = :userId
        AND f.status = 'PENDING'
        AND (:firstPage = true
            OR (:withoutTimestamp = false AND (f.createdAt < :cursor OR f.createdAt IS NULL OR (f.createdAt = :cursor AND f.id.senderId < :cursorId)))
            OR (:withoutTimestamp = true AND f.createdAt IS NULL AND f.id.senderId < :cursorId))
      ORDER BY f.createdAt DESC NULLS LAST, f.id.senderId DESC
      """)
  List<Friendship> findReceivedRequests(@Param("userId") Long userId, @Param("cursor") Instant cursor,
      @Param("cursorId") Long cursorId, @Param("firstPage") boolean firstPage, @Param("withoutTimestamp") boolean withoutTimestamp,
      Pageable pageable);

  @EntityGraph(attributePaths = { "receiver" })
  @Query("""
      SELECT f
      FROM Friendship f
      WHERE f.sender.id = :userId
        AND f.status = 'PENDING'
        AND (:firstPage = true
            OR (:withoutTimestamp = false AND (f.createdAt < :cursor OR f.createdAt IS NULL OR (f.createdAt = :cursor AND f.id.receiverId < :cursorId)))
            OR (:withoutTimestamp = true AND f.createdAt IS NULL AND f.id.receiverId < :cursorId))
      ORDER BY f.createdAt DESC NULLS LAST, f.id.receiverId DESC
      """)
  List<Friendship> findSentRequests(@Param("userId") Long userId, @Param("cursor") Instant cursor,
      @Param("cursorId") Long cursorId, @Param("firstPage") boolean firstPage, @Param("withoutTimestamp") boolean withoutTimestamp, Pageable pageable);
}
