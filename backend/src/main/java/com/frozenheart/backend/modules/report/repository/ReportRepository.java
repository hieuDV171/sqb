package com.frozenheart.backend.modules.report.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Report;
import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.core.entity.socialinteraction.ViolationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findFirstByReporterIdAndTargetTypeAndTargetIdAndStatusOrderByIdDesc(
            Long reporterId, String targetType, Long targetId, ReportStatus status);

    long countByReporterIdAndCreatedAtAfter(Long reporterId, Instant cutoff);

    boolean existsByPrivateEvidenceKey(String privateEvidenceKey);

    @Query("SELECT r.privateEvidenceKey FROM Report r WHERE r.privateEvidenceKey IN :keys")
    Set<String> findAttachedEvidenceKeys(Collection<String> keys);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findForReview(Long id);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.reporter LEFT JOIN FETCH r.reviewer WHERE r.id = :id")
    Optional<Report> findDetails(Long id);

    @Query("""
            SELECT r FROM Report r WHERE r.id < :after
              AND (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:violationType IS NULL OR r.violationType = :violationType)
            ORDER BY r.id DESC
            """)
    List<Report> findQueue(ReportStatus status, String targetType, ViolationType violationType, Long after,
                           Pageable pageable);

    @Query("""
            SELECT r FROM Report r WHERE r.id < :after AND r.id <> :excluded
              AND r.targetType = :targetType AND r.targetId = :targetId
            ORDER BY r.id DESC
            """)
    List<Report> findRelated(String targetType, Long targetId, Long excluded, Long after, Pageable pageable);

    interface PendingCount {
        String getTargetType();
        Long getTargetId();
        long getTotal();
    }

    @Query("""
            SELECT r.targetType AS targetType, r.targetId AS targetId, COUNT(r) AS total
            FROM Report r WHERE r.status = 'PENDING' AND r.targetId IN :ids AND r.targetType IN :types
            GROUP BY r.targetType, r.targetId
            """)
    List<PendingCount> countPendingForTargets(Collection<Long> ids, Collection<String> types);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Report r WHERE r.status = 'RESOLVED' AND r.reviewedAt < :cutoff
              AND (r.snapshot IS NOT NULL OR r.privateEvidenceKey IS NOT NULL)
            ORDER BY r.reviewedAt, r.id
            """)
    List<Report> findExpiredEvidence(Instant cutoff, Pageable pageable);
}
