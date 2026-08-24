package com.frozenheart.backend.modules.ai.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.ai.AiChatMessage;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findBySessionSessionIdAndSessionUserIdOrderByCreatedAtAsc(String sessionId, Long userId);

    @Query("SELECT m FROM AiChatMessage m WHERE m.session.sessionId = :sessionId " +
           "AND m.session.user.id = :userId " +
           "AND (:after IS NULL OR m.id < :after) " +
           "ORDER BY m.id DESC")
    List<AiChatMessage> findMessagesWithCursor(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId,
            @Param("after") Long after,
            Pageable pageable
    );
}
