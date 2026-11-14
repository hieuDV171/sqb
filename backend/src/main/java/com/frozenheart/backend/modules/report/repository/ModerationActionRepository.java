package com.frozenheart.backend.modules.report.repository;

import com.frozenheart.backend.core.entity.socialinteraction.ModerationAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {
    @Query("""
            SELECT a FROM ModerationAction a JOIN FETCH a.admin
            WHERE a.targetType = :targetType AND a.targetId = :targetId ORDER BY a.id DESC
            """)
    List<ModerationAction> findHistory(String targetType, Long targetId, Pageable pageable);

    @Query("""
            SELECT a.id FROM ModerationAction a WHERE a.targetType = 'POST' AND a.searchSyncedAt IS NULL
            ORDER BY a.id
            """)
    List<Long> findPendingPostSync(Pageable pageable);
}
