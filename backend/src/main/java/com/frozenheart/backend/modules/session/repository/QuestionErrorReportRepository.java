package com.frozenheart.backend.modules.session.repository;

import com.frozenheart.backend.core.entity.socialinteraction.QuestionErrorReport;
import com.frozenheart.backend.core.entity.socialinteraction.UserRatingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionErrorReportRepository extends JpaRepository<QuestionErrorReport, UserRatingId> {
    @Query("SELECT r.id.ratedQuestionId FROM QuestionErrorReport r WHERE r.id.userId = :userId AND r.id.ratedQuestionId IN :questionIds")
    List<Long> findReportedQuestionIds(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    @Query("""
            SELECT r FROM QuestionErrorReport r
            JOIN FETCH r.question q
            JOIN FETCH q.session s
            JOIN FETCH s.courseClass cc
            JOIN FETCH cc.subject
            LEFT JOIN FETCH s.proposer
            WHERE r.id = :id
            """)
    Optional<QuestionErrorReport> findByIdWithReviewContext(@Param("id") UserRatingId id);
}
