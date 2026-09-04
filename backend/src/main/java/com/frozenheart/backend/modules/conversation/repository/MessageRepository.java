package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        SELECT COUNT(m)
        FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.sender.id != :userId
          AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)
        """)
    int countUnreadMessages(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("""
        SELECT m
        FROM Message m
        JOIN FETCH m.sender s
        LEFT JOIN FETCH m.replyToMessage r
        LEFT JOIN FETCH r.sender rs
        WHERE m.conversation.id = :conversationId
          AND m.id < :cursor
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

    @Query("SELECT m FROM Message m JOIN FETCH m.sender s JOIN FETCH m.conversation c WHERE m.id = :id")
    java.util.Optional<Message> findByIdFetchSenderAndConversationForSearch(@Param("id") Long id);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender s JOIN FETCH m.conversation c WHERE m.id IN :ids")
    List<Message> findByIdInFetchSenderAndConversationForSearch(@Param("ids") List<Long> ids);

}
