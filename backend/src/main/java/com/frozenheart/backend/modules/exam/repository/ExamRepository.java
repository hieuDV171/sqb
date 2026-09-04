package com.frozenheart.backend.modules.exam.repository;

import com.frozenheart.backend.core.entity.session.Exam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("""
        SELECT e FROM Exam e
        LEFT JOIN FETCH e.subject
        LEFT JOIN FETCH e.lecturer
        LEFT JOIN FETCH e.examQuestions eq
        LEFT JOIN FETCH eq.question q
        LEFT JOIN FETCH q.topic
        LEFT JOIN FETCH q.ownedMedias
        WHERE e.id = :id
    """)
    Optional<Exam> findWithDetailsById(@Param("id") Long id);

    @Query("""
        SELECT e FROM Exam e
        LEFT JOIN FETCH e.subject
        WHERE e.lecturer.id = :lecturerId
          AND (:after IS NULL OR e.id < :after)
          AND (:subjectId IS NULL OR e.subject.id = :subjectId)
        ORDER BY e.id DESC
    """)
    List<Exam> findMyExamsCursor(
            @Param("lecturerId") Long lecturerId,
            @Param("subjectId") Long subjectId,
            @Param("after") Long after,
            Pageable pageable
    );

}
