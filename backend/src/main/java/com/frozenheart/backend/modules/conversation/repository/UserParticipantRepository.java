package com.frozenheart.backend.modules.conversation.repository;

import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.conversation.UserParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserParticipantRepository extends JpaRepository<UserParticipant, UserParticipantId> {

    Optional<UserParticipant> findByUserIdAndConversationId(Long userId, Long conversationId);

    @Query("""
        SELECT p
        FROM UserParticipant p
        JOIN FETCH p.user u
        WHERE p.conversation.id = :conversationId
          AND p.leftAt IS NULL
        """)
    List<UserParticipant> findByConversationIdAndLeftAtIsNull(@Param("conversationId") Long conversationId);

    @Query("""
        SELECT p
        FROM UserParticipant p
        JOIN FETCH p.user u
        WHERE p.conversation.id = :conversationId
          AND p.user.id IN :userIds
        """)
    List<UserParticipant> findByConversationIdAndUserIdIn(
            @Param("conversationId") Long conversationId,
            @Param("userIds") Collection<Long> userIds);

    @Query("""
        SELECT p
        FROM UserParticipant p
        JOIN FETCH p.user u
        WHERE p.conversation.id IN :conversationIds
          AND p.leftAt IS NULL
        """)
    List<UserParticipant> findActiveParticipantsForConversations(@Param("conversationIds") Collection<Long> conversationIds);

    boolean existsByUserIdAndConversationIdAndLeftAtIsNull(Long userId, Long conversationId);
}
