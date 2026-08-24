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

    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.ownedMedias " +
            "WHERE q.session.id = :sessionId " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findQuestionsBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.ownedMedias WHERE q.session.proposer.id = :userId " +
            "AND q.session.status = :status " +
            "AND (:subjectId IS NULL OR q.session.subject.id = :subjectId) " +
            "AND (:after IS NULL OR q.id < :after) " +
            "ORDER BY q.id DESC")
    List<Question> findUserQuestionsWithCursor(@Param("userId") Long userId,
                                               @Param("status") SessionStatus status,
                                               @Param("subjectId") Long subjectId,
                                               @Param("after") Long after,
                                               Pageable pageable);

    @Modifying
    @Query("UPDATE Question q SET q.reactCount = COALESCE(q.reactCount, 0) + :delta WHERE q.id = :questionId")
    int incrementReactCount(@Param("questionId") Long questionId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Question q SET q.commentCount = COALESCE(q.commentCount, 0) + :delta WHERE q.id = :questionId")
    int incrementCommentCount(@Param("questionId") Long questionId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Question q SET q.avgRating = :avgRating, q.ratingCount = :ratingCount WHERE q.id = :questionId")
    int updateRatingStats(@Param("questionId") Long questionId, @Param("avgRating") double avgRating, @Param("ratingCount") int ratingCount);

    @Query("SELECT q FROM Question q JOIN FETCH q.session WHERE q.id = :questionId")
    Optional<Question> findByIdFetchSession(@Param("questionId") Long questionId);

    @Query("SELECT q FROM Question q JOIN FETCH q.session s LEFT JOIN FETCH s.subject WHERE q.id = :questionId")
    Optional<Question> findByIdFetchSessionAndSubject(@Param("questionId") Long questionId);

    long countBySessionSubjectIdAndStatus(Long subjectId, QuestionStatus status);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.ownedMedias WHERE q.status = :status")
    List<Question> findByStatusFetchMedias(@Param("status") QuestionStatus status);

}
