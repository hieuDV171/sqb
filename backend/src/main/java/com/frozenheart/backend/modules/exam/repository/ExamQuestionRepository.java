package com.frozenheart.backend.modules.exam.repository;

import com.frozenheart.backend.core.entity.session.ExamQuestion;
import com.frozenheart.backend.core.entity.session.ExamQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestionId> {

    @Query("""
        SELECT eq FROM ExamQuestion eq
        JOIN FETCH eq.question q
        LEFT JOIN FETCH q.topic
        WHERE eq.exam.id = :examId
        ORDER BY eq.displayOrder ASC
    """)
    List<ExamQuestion> findByExamIdWithQuestionAndTopic(@Param("examId") Long examId);

    @Query("""
        SELECT eq FROM ExamQuestion eq
        JOIN FETCH eq.exam e
        JOIN FETCH eq.question q
        LEFT JOIN FETCH q.topic
        WHERE e.id IN :examIds
        ORDER BY eq.displayOrder ASC
    """)
    List<ExamQuestion> findByExamsIdWithQuestionAndTopic(@Param("examIds") List<Long> examIds);
}
