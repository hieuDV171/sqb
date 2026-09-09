package com.frozenheart.backend.modules.user.repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserBlock;
import com.frozenheart.backend.core.entity.socialinteraction.UserBlockId;
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
public interface BlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM UserBlock b
            WHERE (b.blocker.id = :user1 AND b.blocked.id = :user2)
               OR (b.blocker.id = :user2 AND b.blocked.id = :user1)
            """)
    boolean isBlockedBetween(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("""
            SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END
            FROM UserBlock b
            WHERE (b.blocker.id = :userId AND b.blocked.id IN :targetIds)
               OR (b.blocked.id = :userId AND b.blocker.id IN :targetIds)
            """)
    Set<Long> findBlockedUserIdsAmong(
            @Param("userId") Long userId,
            @Param("targetIds") Collection<Long> targetIds);

    @Query("""
            SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END
            FROM UserBlock b
            WHERE b.blocker.id = :userId OR b.blocked.id = :userId
            """)
    Set<Long> findAllBlockedUserIds(@Param("userId") Long userId);

    int countByBlockerId(Long blockerId);

    @EntityGraph(attributePaths = { "blocked" })
    @Query("""
            SELECT b
            FROM UserBlock b
            WHERE b.blocker.id = :blockerId
              AND b.createdAt < :cursor
            ORDER BY b.createdAt DESC
            """)
    List<UserBlock> findBlockedUsersCursor(@Param("blockerId") Long blockerId, @Param("cursor") Instant cursor,
            Pageable pageable);
}
