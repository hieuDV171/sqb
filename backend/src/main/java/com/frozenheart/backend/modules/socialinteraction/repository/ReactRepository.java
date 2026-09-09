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
import java.util.Set;

@Repository
public interface ReactRepository extends JpaRepository<React, Long> {

    Optional<React> findByUserIdAndTargetTypeAndTargetId(Long userId, InteractionTargetType targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, InteractionTargetType targetType, Long targetId);

    @Query("""
            SELECT r.targetId
            FROM React r
            WHERE r.user.id = :userId
              AND r.targetType = :targetType
              AND r.targetId IN :targetIds
            """)
    Set<Long> findLikedTargetIds(
            @Param("userId") Long userId,
            @Param("targetType") InteractionTargetType targetType,
            @Param("targetIds") Collection<Long> targetIds
    );

    List<React> findByTargetTypeAndTargetId(InteractionTargetType targetType, Long targetId);

    int countByTargetTypeAndTargetId(InteractionTargetType targetType, Long targetId);
}
