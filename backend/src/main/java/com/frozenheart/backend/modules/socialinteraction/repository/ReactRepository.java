package com.frozenheart.backend.modules.socialinteraction.repository;

import com.frozenheart.backend.core.entity.socialinteraction.React;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactRepository extends JpaRepository<React, Long> {

    Optional<React> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    List<React> findByTargetTypeAndTargetId(String targetType, Long targetId);

    int countByTargetTypeAndTargetId(String targetType, Long targetId);
}
