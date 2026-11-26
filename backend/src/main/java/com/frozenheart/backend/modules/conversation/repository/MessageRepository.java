package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    interface UnreadCount {
        Long getConversationId();
        Long getUnreadCount();
    }

    @Query("""
        SELECT m.conversation.id AS conversationId, COUNT(m) AS unreadCount
        FROM Message m
        JOIN UserParticipant p ON p.conversation.id = m.conversation.id AND p.user.id = :userId
        WHERE m.conversation.id IN :conversationIds
          AND p.leftAt IS NULL
          AND m.sender.id != :userId
          AND (m.messageType IS NULL OR m.messageType <> com.frozenheart.backend.core.entity.conversation.MessageType.SYSTEM)
          AND (p.lastReadMessage IS NULL OR m.id > p.lastReadMessage.id)
          AND NOT EXISTS (
              SELECT 1 FROM UserDeletedMessageOnlyMe d
              WHERE d.id.userId = :userId AND d.id.messageId = m.id
          )
        GROUP BY m.conversation.id
        """)
    List<UnreadCount> countUnreadMessages(
            @Param("conversationIds") Collection<Long> conversationIds,
            @Param("userId") Long userId);

    @Query("""
        SELECT m
        FROM Message m
        LEFT JOIN FETCH m.sender
        LEFT JOIN FETCH m.replyToMessage r
        LEFT JOIN FETCH r.sender rs
        LEFT JOIN FETCH r.conversation
        WHERE m.conversation.id = :conversationId
          AND m.id < :cursor
          AND (m.sender IS NOT NULL OR m.messageType = com.frozenheart.backend.core.entity.conversation.MessageType.SYSTEM)
          AND NOT EXISTS (
              SELECT 1
              FROM UserDeletedMessageOnlyMe d
              WHERE d.id.userId = :userId
                AND d.id.messageId = m.id
          )
        ORDER BY m.id DESC
        """)
    List<Message> findMessagesCursor(
        @Param("conversationId") Long conversationId,
        @Param("userId") Long userId,
        @Param("cursor") Long cursor,
        Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.sender
        WHERE m.id = :id AND m.conversation.id = :conversationId
          AND (m.messageType IS NULL OR m.messageType <> com.frozenheart.backend.core.entity.conversation.MessageType.SYSTEM)
        """)
    Optional<Message> findReplyTarget(@Param("id") Long id, @Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender s JOIN FETCH m.conversation c WHERE m.id = :id")
    java.util.Optional<Message> findByIdFetchSenderAndConversationForSearch(@Param("id") Long id);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender s JOIN FETCH m.conversation c WHERE m.id IN :ids")
    List<Message> findByIdInFetchSenderAndConversationForSearch(@Param("ids") List<Long> ids);

    @Query("SELECT m FROM Message m WHERE m.deletedAt IS NOT NULL AND m.deletedAt < :cutoff AND m.mediaUrls IS NOT NULL")
    List<Message> findRevokedMessagesWithMediaOlderThan(@Param("cutoff") Instant cutoff, Pageable pageable);

}
