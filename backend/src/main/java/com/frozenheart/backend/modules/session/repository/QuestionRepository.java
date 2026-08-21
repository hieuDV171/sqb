package com.frozenheart.backend.modules.session.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.session.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySessionId(Long sessionId);

    @Query("SELECT q, up.fullName FROM Question q " +
            "LEFT JOIN FETCH q.ownedMedias " +
            "LEFT JOIN q.reviewer r " +
            "LEFT JOIN UserProfile up ON up.user = r " +
            "WHERE q.session.id = :sessionId " +
            "ORDER BY q.displayOrder ASC")
    List<Object[]> findQuestionsWithReviewerFullNameBySessionId(@Param("sessionId") Long sessionId);

}

