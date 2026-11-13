package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.Conversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Conversation c WHERE c.id = :id")
  Optional<Conversation> findForMembershipUpdate(@Param("id") Long id);

  @Query("""
      SELECT c
      FROM Conversation c
      JOIN c.participants p1
      JOIN c.participants p2
      WHERE c.type = 'DIRECT'
        AND p1.user.id = :user1Id
        AND p2.user.id = :user2Id
      """)
  Optional<Conversation> findDirectConversationBetween(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

  @Query("""
      SELECT c
      FROM Conversation c
      JOIN c.participants p
      WHERE p.user.id = :userId
        AND p.leftAt IS NULL
        AND ((:isHidden = FALSE AND p.hiddenAt IS NULL) OR (:isHidden = TRUE AND p.hiddenAt IS NOT NULL))
        AND (
          (:firstPage = true)
          OR (:withoutMessage = true AND c.lastMessageAt IS NULL AND c.id < :cursorId)
          OR (:withoutMessage = false AND (c.lastMessageAt IS NULL OR c.lastMessageAt < :cursor
            OR (c.lastMessageAt = :cursor AND c.id < :cursorId)))
        )
      ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
      """)
  List<Conversation> findUserConversationsCursor(
      @Param("userId") Long userId,
      @Param("isHidden") boolean isHidden,
      @Param("cursor") Instant cursor,
      @Param("cursorId") Long cursorId,
      @Param("firstPage") boolean firstPage,
      @Param("withoutMessage") boolean withoutMessage,
      Pageable pageable);

  @Query("""
      SELECT COUNT(c)
      FROM Conversation c
      JOIN c.participants p
      WHERE p.user.id = :userId
        AND p.leftAt IS NULL
        AND ((:isHidden = FALSE AND p.hiddenAt IS NULL) OR (:isHidden = TRUE AND p.hiddenAt IS NOT NULL))
      """)
  int countUserConversations(
      @Param("userId") Long userId,
      @Param("isHidden") boolean isHidden);
}
