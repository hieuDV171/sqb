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
                            cc.id AS courseClassId,
                            cc.classCode AS classCode,
                            semester.id AS semesterId,
                            semester.name AS semesterName,
                            s.status AS status,
                            COUNT(q) AS questionCount,
                            SUM(CASE WHEN q.status = com.frozenheart.backend.core.entity.session.QuestionStatus.APPROVED THEN 1 ELSE 0 END) AS approvedQuestionCount,
                            s.createdAt AS createdAt,
                            s.reactCount AS reactCount,
                            s.commentCount AS commentCount
                        FROM Session s
                        JOIN s.courseClass cc
                        JOIN cc.subject subject
                        JOIN cc.semester semester
                        LEFT JOIN s.questions q
                        WHERE s.proposer.id = :proposerId
                          AND (:subjectId IS NULL OR subject.id = :subjectId)
                          AND (:status IS NULL OR s.status = :status)
                          AND (:after IS NULL OR s.id < :after)
                          AND (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(s.sessionCode)) > 0
                               OR LOCATE(LOWER(:keyword), LOWER(s.title)) > 0
                               OR LOCATE(LOWER(:keyword), LOWER(s.content)) > 0
                               OR LOCATE(LOWER(:keyword), LOWER(subject.name)) > 0
                               OR LOCATE(LOWER(:keyword), LOWER(subject.code)) > 0)
                        GROUP BY
                            s.id,
                            s.sessionCode,
                            s.title,
                            s.content,
                            subject.id,
                            subject.name,
                            subject.code,
                            cc.id,
                            cc.classCode,
                            semester.id,
                            semester.name,
                            s.status,
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
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT s.id AS sessionId, s.sessionCode AS sessionCode, s.title AS title,
                               s.content AS content, subject.id AS subjectId, subject.name AS subjectName,
                               s.status AS status, (SELECT COUNT(q) FROM Question q WHERE q.session = s) AS questionCount,
                               s.createdAt AS createdAt, s.reactCount AS reactCount, s.commentCount AS commentCount
                        FROM Session s
                        JOIN s.courseClass cc
                        JOIN cc.subject subject
                        LEFT JOIN s.proposer proposer
                        WHERE s.id = :sessionId AND LOWER(s.sessionCode) = LOWER(:code)
                          AND (s.status = com.frozenheart.backend.core.entity.session.SessionStatus.RESOLVED
                               OR proposer.id = :viewerId)
                        """)
        Optional<SubmissionProjection> findVisibleSessionByCode(@Param("sessionId") Long sessionId,
                        @Param("code") String code, @Param("viewerId") Long viewerId);

        @Query("""
                        SELECT s.id AS sessionId, s.sessionCode AS sessionCode, s.title AS title,
                               s.content AS content, subject.name AS subjectName, subject.code AS subjectCode,
                               (SELECT COUNT(q) FROM Question q WHERE q.session = s) AS questionCount,
                               s.createdAt AS createdAt, s.reactCount AS reactCount, s.commentCount AS commentCount
                        FROM Session s
                        JOIN s.courseClass cc
                        JOIN cc.subject subject
                        WHERE s.status = com.frozenheart.backend.core.entity.session.SessionStatus.RESOLVED
                          AND (:subjectId IS NULL OR subject.id = :subjectId)
                          AND (:after IS NULL OR s.id < :after)
                          AND (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(s.title)) > 0
                               OR LOCATE(LOWER(:keyword), LOWER(s.sessionCode)) > 0)
                        ORDER BY s.id DESC
                        """)
        List<SubmissionProjection> findPracticeCatalog(@Param("subjectId") Long subjectId,
                        @Param("keyword") String keyword, @Param("after") Long after, Pageable pageable);

        @Query("SELECT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject JOIN FETCH cc.semester LEFT JOIN FETCH cc.lecturer WHERE s.id = :sessionId")
        Optional<Session> findByIdFetchCourseClass(@Param("sessionId") Long sessionId);

        @Query("SELECT s FROM Session s LEFT JOIN FETCH s.proposer WHERE s.id = :sessionId")
        Optional<Session> findByIdFetchProposer(@Param("sessionId") Long sessionId);

        @Query("SELECT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject JOIN FETCH cc.semester LEFT JOIN FETCH cc.lecturer LEFT JOIN FETCH s.proposer LEFT JOIN FETCH s.reviewer WHERE s.id = :sessionId")
        Optional<Session> findByIdFetchCourseClassProposerAndReviewer(@Param("sessionId") Long sessionId);

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject JOIN FETCH cc.semester LEFT JOIN FETCH cc.lecturer LEFT JOIN FETCH s.questions q LEFT JOIN FETCH q.ownedMedias WHERE s.id = :sessionId")
        Optional<Session> findByIdFetchCourseClassAndQuestions(@Param("sessionId") Long sessionId);

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject sub JOIN FETCH cc.semester LEFT JOIN FETCH s.proposer p WHERE s.status = com.frozenheart.backend.core.entity.session.SessionStatus.RESOLVED")
        List<Session> findAllResolvedSessionsForSearch();

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject sub JOIN FETCH cc.semester LEFT JOIN FETCH s.proposer p WHERE s.id = :id")
        Optional<Session> findByIdFetchCourseClassAndProposerForSearch(@Param("id") Long id);

        @Query("SELECT DISTINCT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject sub JOIN FETCH cc.semester LEFT JOIN FETCH s.proposer p WHERE s.id IN :ids")
        List<Session> findByIdInFetchCourseClassAndProposerForSearch(@Param("ids") List<Long> ids);

        @Query("SELECT s FROM Session s JOIN FETCH s.courseClass cc JOIN FETCH cc.subject WHERE s.id IN :ids")
        List<Session> findByIdInFetchSubjects(@Param("ids") Collection<Long> ids);

        @Query(value = """
                        SELECT id FROM sessions
                        WHERE id IN (:ids) AND (:pendingOnly = false OR status = 'PENDING')
                        ORDER BY id FOR UPDATE
                        """, nativeQuery = true)
        List<Long> lockReviewSessions(@Param("ids") Collection<Long> ids,
                                      @Param("pendingOnly") boolean pendingOnly);

        @Query("""
                SELECT s FROM Session s JOIN FETCH s.courseClass cc
                JOIN FETCH cc.subject subject JOIN FETCH cc.semester semester JOIN FETCH s.proposer
                WHERE s.status IN ('PENDING', 'REVIEWING')
                  AND (:lecturerId IS NULL OR cc.lecturer.id = :lecturerId)
                  AND (:courseClassId IS NULL OR cc.id = :courseClassId)
                  AND (:after IS NULL OR s.id < :after)
                  AND (:before IS NULL OR s.id > :before)
                  AND (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(s.title)) > 0
                    OR LOCATE(LOWER(:keyword), LOWER(s.sessionCode)) > 0
                    OR LOCATE(LOWER(:keyword), LOWER(subject.code)) > 0
                    OR LOCATE(LOWER(:keyword), LOWER(subject.name)) > 0
                    OR LOCATE(LOWER(:keyword), LOWER(cc.classCode)) > 0
                    OR LOCATE(LOWER(:keyword), LOWER(semester.name)) > 0
                    OR LOCATE(:keyword, CAST(s.id AS String)) > 0)
                """)
        List<Session> findReviewableSessionsFetchCourseClassAndProposerWithCursor(
                        @Param("lecturerId") Long lecturerId,
                        @Param("courseClassId") Long courseClassId,
                        @Param("after") Long after, @Param("before") Long before,
                        @Param("keyword") String keyword, Pageable pageable);

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
                SELECT s.courseClass.id, COUNT(DISTINCT s.proposer.id)
                FROM Session s
                WHERE s.courseClass.id IN :classIds
                  AND s.createdAt >= :startDate AND s.createdAt < :endDate
                GROUP BY s.courseClass.id
                """)
        List<Object[]> countDistinctProposersByCourseClassesBetween(
                @Param("classIds") Collection<Long> classIds,
                @Param("startDate") Instant startDate,
                @Param("endDate") Instant endDate);

        List<Session> findByStatusAndCreatedAtBefore(SessionStatus status, Instant cutoff);

        @Query("SELECT s.id FROM Session s " +
                        "WHERE s.status = :status AND s.createdAt < :cutoff " +
                        "ORDER BY s.id ASC")
        List<Long> findIdsByStatusAndCreatedAtBefore(
                        @Param("status") SessionStatus status,
                        @Param("cutoff") Instant cutoff,
                        Pageable pageable);

        @Query("SELECT DISTINCT s FROM Session s " +
                        "LEFT JOIN FETCH s.proposer " +
                        "JOIN FETCH s.courseClass cc " +
                        "JOIN FETCH cc.subject " +
                        "JOIN FETCH cc.semester " +
                        "LEFT JOIN FETCH s.questions " +
                        "WHERE s.id IN :ids")
        List<Session> findByIdInFetchAutomatedReviewDetails(@Param("ids") Collection<Long> ids);

        int countByReviewerIdAndStatus(Long reviewerId, SessionStatus status);

}
