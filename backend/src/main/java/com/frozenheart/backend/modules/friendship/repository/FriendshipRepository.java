package com.frozenheart.backend.modules.friendship.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Friendship;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipId;
import com.frozenheart.backend.core.entity.socialinteraction.FriendshipStatus;
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
public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query("""
        SELECT f
        FROM Friendship f
        WHERE (f.sender.id = :userId1 AND f.receiver.id = :userId2)
           OR (f.sender.id = :userId2 AND f.receiver.id = :userId1)
        """)
    Optional<Friendship> findFriendshipsBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

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

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
          AND f.status = 'ACCEPTED'
        ORDER BY f.acceptedAt DESC
        """)
    List<Friendship> findAcceptedFriendsSimple(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
          AND f.status = 'ACCEPTED'
          AND f.acceptedAt < :cursor
        ORDER BY f.acceptedAt DESC
        """)
    List<Friendship> findAcceptedFriendsCursor(@Param("userId") Long userId, @Param("cursor") LocalDateTime cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.receiver.id = :userId
          AND f.status = 'PENDING'
          AND f.createdAt < :cursor
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> findReceivedRequests(@Param("userId") Long userId, @Param("cursor") LocalDateTime cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"receiver"})
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.sender.id = :userId
          AND f.status = 'PENDING'
          AND f.createdAt < :cursor
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> findSentRequests(@Param("userId") Long userId, @Param("cursor") LocalDateTime cursor, Pageable pageable);
}
