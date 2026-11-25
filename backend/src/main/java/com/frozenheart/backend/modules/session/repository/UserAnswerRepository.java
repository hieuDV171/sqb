package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserAnswer;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswerId;
import com.frozenheart.backend.modules.session.dto.AnswerOptionCountProjection;
import com.frozenheart.backend.modules.session.dto.AnswerStatisticsProjection;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, UserAnswerId> {

    boolean existsByIdUserIdAndIdQuestionId(Long userId, Long questionId);

    Optional<UserAnswer> findByIdUserIdAndIdQuestionId(Long userId, Long questionId);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.id.userId = :userId AND ua.id.questionId IN :questionIds")
    List<UserAnswer> findByUserIdAndQuestionIdIn(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    long countByIdQuestionId(Long questionId);

    @Query("""
            SELECT
            COUNT(ua) AS totalAnswers,
            COALESCE(SUM(CASE WHEN ua.isCorrect = true THEN 1L ELSE 0L END), 0L) AS correctAnswers
            FROM UserAnswer ua WHERE ua.id.questionId = :questionId
            """)
    AnswerStatisticsProjection getAnswerStatsSummary(@Param("questionId") Long questionId);

    @Query(value = """
            SELECT item.value ->> 'key' AS "optionKey", COUNT(*) AS "selectionCount"
            FROM user_answers ua
            CROSS JOIN LATERAL jsonb_array_elements(
                CASE WHEN jsonb_typeof(ua.selected_options) = 'array' THEN ua.selected_options
                     ELSE CAST('[]' AS jsonb) END
            ) AS item(value)
            WHERE ua.question_id = :questionId AND item.value ->> 'key' IS NOT NULL
            GROUP BY item.value ->> 'key'
            """, nativeQuery = true)
    List<AnswerOptionCountProjection> countSelectedOptionsByQuestionId(@Param("questionId") Long questionId);

}

