package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswer;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswerId;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, UserAnswerId> {

    boolean existsByIdUserIdAndIdQuestionId(Long userId, Long questionId);

    Optional<UserAnswer> findByIdUserIdAndIdQuestionId(Long userId, Long questionId);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.id.userId = :userId AND ua.id.questionId IN :questionIds")
    List<UserAnswer> findByUserIdAndQuestionIdIn(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    long countByIdQuestionId(Long questionId);

    @Query("SELECT " +
           "COUNT(ua), " +
           "SUM(CASE WHEN ua.isCorrect = true THEN 1L ELSE 0L END), " +
           "COALESCE(AVG(ua.timeSpentSeconds), 0.0) " +
           "FROM UserAnswer ua WHERE ua.id.questionId = :questionId")
    Object[] getAnswerStatsSummary(@Param("questionId") Long questionId);

    @Query("SELECT ua.selectedOptions FROM UserAnswer ua WHERE ua.id.questionId = :questionId")
    List<List<QuestionOption>> findSelectedOptionsByQuestionId(@Param("questionId") Long questionId);

}

