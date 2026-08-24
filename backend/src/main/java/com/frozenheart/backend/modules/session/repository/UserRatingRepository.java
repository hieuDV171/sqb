package com.frozenheart.backend.modules.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.socialinteraction.UserRatingId;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, UserRatingId> {

    boolean existsByIdUserIdAndIdRatedQuestionId(Long userId, Long ratedQuestionId);

    Optional<UserRating> findByIdUserIdAndIdRatedQuestionId(Long userId, Long ratedQuestionId);

    @Query("SELECT ur FROM UserRating ur JOIN FETCH ur.ratedQuestion q LEFT JOIN FETCH q.session s LEFT JOIN FETCH s.subject sub WHERE ur.id.userId = :userId AND ur.id.ratedQuestionId = :ratedQuestionId")
    Optional<UserRating> findByUserIdAndRatedQuestionIdFetchQuestionAndSession(@Param("userId") Long userId, @Param("ratedQuestionId") Long ratedQuestionId);

    @Query("SELECT ur FROM UserRating ur WHERE ur.id.userId = :userId AND ur.id.ratedQuestionId IN :questionIds")
    List<UserRating> findByUserIdAndRatedQuestionIdIn(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    long countByIdRatedQuestionId(Long ratedQuestionId);

    @Query("SELECT " +
           "COALESCE(AVG(ur.rating), 0.0), " +
           "COUNT(ur), " +
           "SUM(CASE WHEN ur.rating = 0.0 THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN ur.rating = 1.0 THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN ur.rating = 2.0 THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN ur.rating = 3.0 THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN ur.rating = 4.0 THEN 1L ELSE 0L END) " +
           "FROM UserRating ur WHERE ur.id.ratedQuestionId = :ratedQuestionId")
    Object[] getRatingStatsSummary(@Param("ratedQuestionId") Long ratedQuestionId);

    @Query("SELECT COALESCE(AVG(ur.rating), 0.0), COUNT(ur) FROM UserRating ur WHERE ur.id.ratedQuestionId = :ratedQuestionId")
    Object[] getAvgAndCountByQuestionId(@Param("ratedQuestionId") Long ratedQuestionId);

    @Query("SELECT ur FROM UserRating ur LEFT JOIN FETCH ur.user WHERE ur.id.ratedQuestionId = :ratedQuestionId " +
           "AND (:after IS NULL OR ur.id.userId < :after) ORDER BY ur.id.userId DESC")
    List<UserRating> findRatingsByQuestionIdWithCursor(@Param("ratedQuestionId") Long ratedQuestionId,
                                                       @Param("after") Long after,
                                                       Pageable pageable);

}

