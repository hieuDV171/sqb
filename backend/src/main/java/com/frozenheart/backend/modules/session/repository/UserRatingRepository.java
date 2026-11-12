package com.frozenheart.backend.modules.session.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.socialinteraction.UserRatingId;
import com.frozenheart.backend.modules.session.dto.RatingAverageProjection;
import com.frozenheart.backend.modules.session.dto.RatingStatisticsProjection;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, UserRatingId> {

    boolean existsByIdUserIdAndIdRatedQuestionId(Long userId, Long ratedQuestionId);

    Optional<UserRating> findByIdUserIdAndIdRatedQuestionId(Long userId, Long ratedQuestionId);

    @Query("SELECT ur FROM UserRating ur WHERE ur.id.userId = :userId AND ur.id.ratedQuestionId IN :questionIds")
    List<UserRating> findByUserIdAndRatedQuestionIdIn(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    long countByIdRatedQuestionId(Long ratedQuestionId);

    @Query("SELECT " +
           "COALESCE(AVG(ur.rating), 0.0) AS avgRating, " +
           "COUNT(ur) AS totalRatings, " +
           "COALESCE(SUM(CASE WHEN ur.rating = 0.0 THEN 1L ELSE 0L END), 0L) AS rating0Count, " +
           "COALESCE(SUM(CASE WHEN ur.rating = 1.0 THEN 1L ELSE 0L END), 0L) AS rating1Count, " +
           "COALESCE(SUM(CASE WHEN ur.rating = 2.0 THEN 1L ELSE 0L END), 0L) AS rating2Count, " +
           "COALESCE(SUM(CASE WHEN ur.rating = 3.0 THEN 1L ELSE 0L END), 0L) AS rating3Count, " +
           "COALESCE(SUM(CASE WHEN ur.rating = 4.0 THEN 1L ELSE 0L END), 0L) AS rating4Count " +
           "FROM UserRating ur WHERE ur.id.ratedQuestionId = :ratedQuestionId")
    RatingStatisticsProjection getRatingStatsSummary(@Param("ratedQuestionId") Long ratedQuestionId);

    @Query("SELECT COALESCE(AVG(ur.rating), 0.0) AS avgRating, COUNT(ur) AS totalRatings " +
           "FROM UserRating ur WHERE ur.id.ratedQuestionId = :ratedQuestionId")
    RatingAverageProjection getAvgAndCountByQuestionId(@Param("ratedQuestionId") Long ratedQuestionId);

    @Query("SELECT ur FROM UserRating ur WHERE ur.id.ratedQuestionId = :ratedQuestionId " +
           "AND (:after IS NULL OR ur.id.userId < :after) ORDER BY ur.id.userId DESC")
    List<UserRating> findRatingsByQuestionIdWithCursor(@Param("ratedQuestionId") Long ratedQuestionId,
                                                       @Param("after") Long after,
                                                       Pageable pageable);

    @Query("""
            SELECT ur.id.ratedQuestionId AS questionId, COUNT(ur) AS ratingCount
            FROM UserRating ur WHERE ur.id.ratedQuestionId IN :questionIds
            GROUP BY ur.id.ratedQuestionId
            """)
    List<QuestionRatingCount> countStarsByQuestionIds(@Param("questionIds") List<Long> questionIds);

    interface QuestionRatingCount {
        Long getQuestionId();
        long getRatingCount();
    }

    @Query("""
            SELECT q.id AS questionId, q.questionCode AS questionCode, q.displayOrder AS displayOrder,
                   COUNT(DISTINCT ur.id.userId) AS ratingCount, AVG(ur.rating) AS averageRating
            FROM Question q
            JOIN Prediction p ON p.targetId = q.session.id AND p.id IN :predictionIds
            LEFT JOIN UserRating ur ON ur.id.ratedQuestionId = q.id
                AND ur.id.userId <> p.gambler.id
                AND ur.createdAt < p.targetDate AND ur.createdAt < :now
            WHERE q.id IN :questionIds
            GROUP BY q.id, q.questionCode, q.displayOrder
            """)
    List<Game3QuestionRating> summarizeGame3Ratings(@Param("predictionIds") Collection<Long> predictionIds,
            @Param("questionIds") Collection<Long> questionIds, @Param("now") Instant now);

    interface Game3QuestionRating {
        Long getQuestionId();
        String getQuestionCode();
        Integer getDisplayOrder();
        long getRatingCount();
        Double getAverageRating();
    }

}

