package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject LEFT JOIN FETCH s.questions " +
           "WHERE s.proposer.id = :proposerId " +
           "AND (:subjectId IS NULL OR s.subject.id = :subjectId) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:after IS NULL OR s.id < :after) " +
           "ORDER BY s.id DESC")
    List<Session> findMySubmissions(
            @Param("proposerId") Long proposerId,
            @Param("subjectId") Long subjectId,
            @Param("status") SessionStatus status,
            @Param("after") Long after,
            Pageable pageable
    );

    @Query("SELECT s FROM Session s JOIN FETCH s.subject WHERE s.id = :sessionId")
    Optional<Session> findByIdWithSubject(@Param("sessionId") Long sessionId);
}

