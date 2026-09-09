package com.frozenheart.backend.modules.badge.repository;

import com.frozenheart.backend.core.entity.badge.UserBadge;
import com.frozenheart.backend.core.entity.badge.UserBadgeId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UserBadgeId> {

    @EntityGraph(attributePaths = {"badge"})
    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    boolean existsByIdUserIdAndIdBadgeId(Long userId, Long badgeId);

    int countByIdUserId(Long userId);

    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.id.badgeId = :badgeId")
    int countByBadgeId(@Param("badgeId") Long badgeId);

    @Query("SELECT ub.id.userId FROM UserBadge ub WHERE ub.id.badgeId = :badgeId AND ub.id.userId IN :userIds")
    Set<Long> findGrantedUserIds(@Param("badgeId") Long badgeId, @Param("userIds") Collection<Long> userIds);

    @Query("SELECT ub FROM UserBadge ub WHERE ub.id.userId IN :userIds AND ub.id.badgeId IN :badgeIds")
    List<UserBadge> findByUserIdsAndBadgeIds(@Param("userIds") Collection<Long> userIds, @Param("badgeIds") Collection<Long> badgeIds);
}
