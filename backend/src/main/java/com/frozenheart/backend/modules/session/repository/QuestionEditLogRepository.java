package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLog;
import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLogStatus;

@Repository
public interface QuestionEditLogRepository extends JpaRepository<QuestionEditLog, Long> {

    List<QuestionEditLog> findByQuestionIdOrderByCreatedAtDesc(Long questionId);

    Optional<QuestionEditLog> findTopByQuestionIdOrderByCreatedAtDesc(Long questionId);
    Optional<QuestionEditLog> findByIdAndQuestionIdAndActorIdAndStatus(
            Long id, Long questionId, Long actorId, QuestionEditLogStatus status);

    List<QuestionEditLog> findByQuestionIdInOrderByCreatedAtDesc(List<Long> questionIds);

    @Query("""
            SELECT l FROM QuestionEditLog l LEFT JOIN FETCH l.actor
            WHERE l.id IN (
                SELECT ranked.logId
                FROM (
                    SELECT candidate.id AS logId, row_number() OVER (
                        PARTITION BY candidate.question.id
                        ORDER BY candidate.createdAt DESC NULLS LAST, candidate.id DESC
                    ) AS logRank
                    FROM QuestionEditLog candidate
                    WHERE candidate.question.id IN :questionIds
                ) ranked
                WHERE ranked.logRank = 1
            )
            """)
    List<QuestionEditLog> findLatestByQuestionIdInFetchActor(@Param("questionIds") List<Long> questionIds);

    @Query("SELECT l FROM QuestionEditLog l JOIN FETCH l.question q JOIN FETCH q.session s JOIN FETCH s.courseClass cc LEFT JOIN FETCH cc.lecturer WHERE l.id = :id")
    Optional<QuestionEditLog> findByIdFetchQuestion(@Param("id") Long id);
}
