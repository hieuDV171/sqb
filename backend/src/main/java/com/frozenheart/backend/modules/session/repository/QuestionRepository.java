package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.SessionStatus;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySessionId(Long sessionId);

    List<Question> findBySessionIdIn(List<Long> sessionIds);

    @Query("""
            SELECT q FROM Question q
            LEFT JOIN FETCH q.ownedMedias
            WHERE q.session.id = :sessionId
            ORDER BY q.displayOrder ASC
            """)
    List<Question> findQuestionsBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
            SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.ownedMedias WHERE q.session.proposer.id = :userId
            AND q.session.status = :status
            AND (:subjectId IS NULL OR q.session.subject.id = :subjectId)
            AND (:after IS NULL OR q.id < :after)
            ORDER BY q.id DESC
            """)
    List<Question> findUserQuestionsWithCursor(@Param("userId") Long userId,
            @Param("status") SessionStatus status,
            @Param("subjectId") Long subjectId,
            @Param("after") Long after,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Question q SET q.reactCount = COALESCE(q.reactCount, 0) + :delta WHERE q.id = :questionId")
    void incrementReactCount(@Param("questionId") Long questionId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Question q SET q.commentCount = COALESCE(q.commentCount, 0) + :delta WHERE q.id = :questionId")
    void incrementCommentCount(@Param("questionId") Long questionId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Question q SET q.avgRating = :avgRating, q.ratingCount = :ratingCount WHERE q.id = :questionId")
    void updateRatingStats(@Param("questionId") Long questionId, @Param("avgRating") double avgRating,
            @Param("ratingCount") int ratingCount);

    @Query("SELECT q FROM Question q JOIN FETCH q.session WHERE q.id = :questionId")
    Optional<Question> findByIdFetchSession(@Param("questionId") Long questionId);

    @Query("SELECT q FROM Question q JOIN FETCH q.session s LEFT JOIN FETCH s.subject LEFT JOIN FETCH s.proposer WHERE q.id = :questionId")
    Optional<Question> findByIdFetchSessionAndSubject(@Param("questionId") Long questionId);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.session s JOIN FETCH s.subject sub LEFT JOIN FETCH q.topic t LEFT JOIN FETCH q.ownedMedias WHERE q.status IN (com.frozenheart.backend.core.entity.session.QuestionStatus.APPROVED, com.frozenheart.backend.core.entity.session.QuestionStatus.REJECTED)")
    List<Question> findAllReviewedQuestionsForSearch();

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.session s JOIN FETCH s.subject sub LEFT JOIN FETCH q.topic t LEFT JOIN FETCH q.ownedMedias WHERE q.id = :id")
    Optional<Question> findByIdFetchDetailsForSearch(@Param("id") Long id);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.session s JOIN FETCH s.subject sub LEFT JOIN FETCH q.topic t LEFT JOIN FETCH q.ownedMedias WHERE q.id IN :ids")
    List<Question> findByIdInFetchDetailsForSearch(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.session s LEFT JOIN FETCH s.subject LEFT JOIN FETCH s.proposer WHERE q.id IN :ids")
    List<Question> findByIdInFetchSessionSubjectAndProposer(@Param("ids") List<Long> ids);

    long countBySessionSubjectIdAndStatus(Long subjectId, QuestionStatus status);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.ownedMedias WHERE q.status = :status")
    List<Question> findByStatusFetchMedias(@Param("status") QuestionStatus status);

    @Query("SELECT q.id FROM Question q WHERE q.status != 'APPROVED'")
    List<Long> findNonApprovedQuestionIds();

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.ownedMedias WHERE q.id IN :ids")
    List<Question> findByIdInFetchMedias(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.session s JOIN FETCH s.subject sub LEFT JOIN FETCH q.ownedMedias WHERE sub.id = :subjectId ORDER BY q.id ASC")
    List<Question> findAllOriginalBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.session s JOIN FETCH s.subject sub LEFT JOIN FETCH q.topic t LEFT JOIN FETCH q.ownedMedias WHERE sub.id = :subjectId AND q.status = com.frozenheart.backend.core.entity.session.QuestionStatus.APPROVED")
    List<Question> findApprovedBySubjectId(@Param("subjectId") Long subjectId);

}
