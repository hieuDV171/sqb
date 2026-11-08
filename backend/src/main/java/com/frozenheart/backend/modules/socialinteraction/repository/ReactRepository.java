package com.frozenheart.backend.modules.socialinteraction.repository;

import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReactRepository extends JpaRepository<React, Long> {

    Optional<React> findByUserIdAndTargetTypeAndTargetId(Long userId, InteractionTargetType targetType, Long targetId);

    List<React> findByTargetTypeAndTargetId(InteractionTargetType targetType, Long targetId);

    @Query("""
            SELECT r
            FROM React r
            WHERE r.user.id = :userId
              AND r.targetType = :targetType
              AND r.targetId IN :targetIds
            """)
    List<React> findUserReactsByTargetIds(
            @Param("userId") Long userId,
            @Param("targetType") InteractionTargetType targetType,
            @Param("targetIds") Collection<Long> targetIds
    );

    int countByTargetTypeAndTargetId(InteractionTargetType targetType, Long targetId);

    @Query("""
            SELECT r.targetId AS targetId, COUNT(r) AS total,
                   SUM(CASE WHEN r.user.id = :viewerId THEN 1 ELSE 0 END) AS viewerCount
            FROM React r
            WHERE r.targetType = com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType.BADGE_AWARD
              AND r.targetId IN :feedIds
            GROUP BY r.targetId
            """)
    List<BadgeCheerCount> countBadgeCheers(@Param("feedIds") Collection<Long> feedIds, @Param("viewerId") Long viewerId);

    interface BadgeCheerCount {
        Long getTargetId();
        long getTotal();
        long getViewerCount();
    }
}
