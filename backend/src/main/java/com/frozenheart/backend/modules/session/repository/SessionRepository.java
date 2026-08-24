package com.frozenheart.backend.modules.session.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            Pageable pageable);

    @Query("SELECT s FROM Session s JOIN FETCH s.subject WHERE s.id = :sessionId")
    Optional<Session> findByIdFetchSubject(@Param("sessionId") Long sessionId);

    @Query("SELECT s, up.fullName FROM Session s JOIN FETCH s.subject LEFT JOIN FETCH s.reviewer r LEFT JOIN UserProfile up ON r.id = up.userId WHERE s.id = :sessionId")
    Optional<Object[]> findByIdFetchSubjectAndReviewer(@Param("sessionId") Long sessionId);

    @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject LEFT JOIN FETCH s.questions q LEFT JOIN FETCH q.ownedMedias WHERE s.id = :sessionId")
    Optional<Session> findByIdFetchSubjectAndQuestions(@Param("sessionId") Long sessionId);

    @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject JOIN FETCH s.proposer " +
            "WHERE s.status = com.frozenheart.backend.core.entity.session.SessionStatus.PENDING " +
            "AND (:subjectId IS NULL OR s.subject.id = :subjectId) " +
            "AND (:after IS NULL OR s.id < :after) " +
            "ORDER BY s.id DESC")
    List<Session> findPendingSessionsFetchSubjectAndProposerWithCursor(
            @Param("subjectId") Long subjectId,
            @Param("after") Long after,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Session s SET s.reactCount = s.reactCount + :delta WHERE s.id = :sessionId")
    int incrementReactCount(@Param("sessionId") Long sessionId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Session s SET s.commentCount = s.commentCount + :delta WHERE s.id = :sessionId")
    int incrementCommentCount(@Param("sessionId") Long sessionId, @Param("delta") int delta);

    @Query("SELECT COUNT(DISTINCT s.proposer.id) FROM Session s WHERE s.createdAt >= :startDate AND s.createdAt < :endDate AND s.proposer IS NOT NULL")
    long countDistinctProposersBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<Session> findByStatusAndCreatedAtBefore(SessionStatus status, LocalDateTime cutoff);

    @Query("SELECT DISTINCT s FROM Session s " +
            "LEFT JOIN FETCH s.proposer " +
            "LEFT JOIN FETCH s.subject " +
            "LEFT JOIN FETCH s.questions q " +
            "LEFT JOIN FETCH q.duplicateWarnings " +
            "WHERE s.status = :status AND s.createdAt < :cutoff " +
            "ORDER BY s.id ASC")
    List<Session> findByStatusAndCreatedAtBeforeFetchDetails(
            @Param("status") SessionStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);

}
