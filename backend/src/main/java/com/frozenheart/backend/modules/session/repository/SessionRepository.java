package com.frozenheart.backend.modules.session.repository;

import java.time.Instant;
import java.util.Collection;
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
import com.frozenheart.backend.modules.session.dto.SubmissionProjection;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

        @Query("""
                        SELECT
                            s.id AS sessionId,
                            s.sessionCode AS sessionCode,
                            s.title AS title,
                            s.content AS content,
                            subject.id AS subjectId,
                            subject.name AS subjectName,
                            subject.code AS subjectCode,
                            COUNT(q) AS questionCount,
                            s.createdAt AS createdAt,
                            s.reactCount AS reactCount,
                            s.commentCount AS commentCount
                        FROM Session s
                        JOIN s.subject subject
                        LEFT JOIN s.questions q
                        WHERE s.proposer.id = :proposerId
                          AND (:subjectId IS NULL OR subject.id = :subjectId)
                          AND (:status IS NULL OR s.status = :status)
                          AND (:after IS NULL OR s.id < :after)
                        GROUP BY
                            s.id,
                            s.sessionCode,
                            s.title,
                            s.content,
                            subject.id,
                            subject.name,
                            subject.code,
                            s.createdAt,
                            s.reactCount,
                            s.commentCount
                        ORDER BY s.id DESC
                        """)
        List<SubmissionProjection> findSubmissions(
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

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject sub LEFT JOIN FETCH s.proposer p WHERE s.status = com.frozenheart.backend.core.entity.session.SessionStatus.RESOLVED")
        List<Session> findAllResolvedSessionsForSearch();

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject sub LEFT JOIN FETCH s.proposer p WHERE s.id = :id")
        Optional<Session> findByIdFetchSubjectAndProposerForSearch(@Param("id") Long id);

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject sub LEFT JOIN FETCH s.proposer p WHERE s.id IN :ids")
        List<Session> findByIdInFetchSubjectAndProposerForSearch(@Param("ids") List<Long> ids);

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.subject sub LEFT JOIN FETCH s.proposer p LEFT JOIN FETCH s.reviewer r WHERE s.id IN :ids")
        List<Session> findByIdInFetchSubjectAndProposerAndReviewer(@Param("ids") Collection<Long> ids);

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
        void incrementReactCount(@Param("sessionId") Long sessionId, @Param("delta") int delta);

        @Modifying
        @Query("UPDATE Session s SET s.commentCount = s.commentCount + :delta WHERE s.id = :sessionId")
        void incrementCommentCount(@Param("sessionId") Long sessionId, @Param("delta") int delta);

        @Query("SELECT COUNT(DISTINCT s.proposer.id) FROM Session s WHERE s.createdAt >= :startDate AND s.createdAt < :endDate AND s.proposer IS NOT NULL")
        long countDistinctProposersBetween(@Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        @Query("""
                SELECT ucc.courseClass.id, COUNT(DISTINCT s.proposer.id)
                FROM Session s
                JOIN com.frozenheart.backend.core.entity.user.UserCourseClass ucc ON s.proposer.id = ucc.user.id
                JOIN ucc.courseClass cc
                WHERE cc.id IN :classIds
                  AND s.subject.id = cc.subject.id
                  AND s.createdAt >= :startDate AND s.createdAt < :endDate
                GROUP BY ucc.courseClass.id
                """)
        List<Object[]> countDistinctProposersByCourseClassesBetween(
                @Param("classIds") Collection<Long> classIds,
                @Param("startDate") Instant startDate,
                @Param("endDate") Instant endDate);

        List<Session> findByStatusAndCreatedAtBefore(SessionStatus status, Instant cutoff);

        @Query("SELECT DISTINCT s FROM Session s " +
                        "LEFT JOIN FETCH s.proposer " +
                        "LEFT JOIN FETCH s.subject " +
                        "LEFT JOIN FETCH s.questions q " +
                        "LEFT JOIN FETCH q.duplicateWarnings " +
                        "WHERE s.status = :status AND s.createdAt < :cutoff " +
                        "ORDER BY s.id ASC")
        List<Session> findByStatusAndCreatedAtBeforeFetchDetails(
                        @Param("status") SessionStatus status,
                        @Param("cutoff") Instant cutoff,
                        Pageable pageable);

        int countByReviewerId(Long reviewerId);

}
