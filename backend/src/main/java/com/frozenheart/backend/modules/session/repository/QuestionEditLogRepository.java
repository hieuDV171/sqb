package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLog;

@Repository
public interface QuestionEditLogRepository extends JpaRepository<QuestionEditLog, Long> {

    List<QuestionEditLog> findByQuestionIdOrderByCreatedAtDesc(Long questionId);

    Optional<QuestionEditLog> findTopByQuestionIdOrderByCreatedAtDesc(Long questionId);

    List<QuestionEditLog> findByQuestionIdInOrderByCreatedAtDesc(List<Long> questionIds);

    @Query("SELECT l FROM QuestionEditLog l LEFT JOIN FETCH l.actor WHERE l.question.id IN :questionIds ORDER BY l.createdAt DESC")
    List<QuestionEditLog> findByQuestionIdInFetchActorOrderByCreatedAtDesc(@Param("questionIds") List<Long> questionIds);

    @Query("SELECT l FROM QuestionEditLog l JOIN FETCH l.question WHERE l.id = :id")
    Optional<QuestionEditLog> findByIdFetchQuestion(@Param("id") Long id);
}
