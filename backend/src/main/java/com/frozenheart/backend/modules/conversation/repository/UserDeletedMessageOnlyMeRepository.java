package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.UserDeletedMessageOnlyMe;
import com.frozenheart.backend.core.entity.conversation.UserDeletedMessageOnlyMeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

@Repository
public interface UserDeletedMessageOnlyMeRepository extends JpaRepository<UserDeletedMessageOnlyMe, UserDeletedMessageOnlyMeId> {

    @Query("""
        SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END
        FROM UserDeletedMessageOnlyMe d
        WHERE d.id.userId = :userId AND d.id.messageId = :messageId
        """)
    boolean existsByUserIdAndMessageId(@Param("userId") Long userId, @Param("messageId") Long messageId);

    @Query("""
        SELECT d.id.messageId FROM UserDeletedMessageOnlyMe d
        WHERE d.id.userId = :userId AND d.id.messageId IN :messageIds
        """)
    Set<Long> findDeletedMessageIds(@Param("userId") Long userId, @Param("messageIds") Collection<Long> messageIds);

    @Query("""
        SELECT d.id.userId FROM UserDeletedMessageOnlyMe d
        WHERE d.id.messageId = :messageId AND d.id.userId IN :userIds
        """)
    Set<Long> findViewersWhoDeleted(@Param("messageId") Long messageId, @Param("userIds") Collection<Long> userIds);
}
