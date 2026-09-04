package com.frozenheart.backend.modules.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.ai.AiChatSession;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    Optional<AiChatSession> findBySessionIdAndUserId(String sessionId, Long userId);

    List<AiChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("""
            SELECT s FROM AiChatSession s WHERE s.user.id = :userId
            AND (:after IS NULL OR s.id < :after)
            ORDER BY s.id DESC
        """)
    List<AiChatSession> findUserSessionsWithCursor(
            @Param("userId") Long userId,
            @Param("after") Long after,
            Pageable pageable
    );
}
