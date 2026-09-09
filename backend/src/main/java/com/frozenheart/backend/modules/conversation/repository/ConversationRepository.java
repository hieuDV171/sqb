package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

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
        AND (c.lastMessageAt IS NULL OR c.lastMessageAt < :cursor)
      ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
      """)
  List<Conversation> findUserConversationsCursor(
      @Param("userId") Long userId,
      @Param("isHidden") boolean isHidden,
      @Param("cursor") Instant cursor,
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
