package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEvidenceRetention {
    private final ReportEvidenceStorage storage;
    private final R2Properties properties;
    private final ReportRepository reports;
    private final TransactionTemplate transactions;

    @Scheduled(cron = "0 15 * * * *", zone = Time.DEFAULT_TIMEZONE)
    public void clean() {
        try {
            storage.cleanStaging();
        } catch (RuntimeException failure) {
            log.warn("Could not check the staging queue for report evidence", failure);
        }
        Instant cutoff = Instant.now().minus(Math.max(1, properties.getReportEvidenceRetentionDays()), ChronoUnit.DAYS);
        transactions.executeWithoutResult(status ->
                reports.findExpiredEvidence(cutoff, PageRequest.of(0, 100)).forEach(report -> {
                    try {
                        if (report.getPrivateEvidenceKey() != null) storage.delete(report.getPrivateEvidenceKey());
                        report.setPrivateEvidenceKey(null);
                        report.setSnapshot(null);
                    } catch (RuntimeException failure) {
                        log.warn("Could not remove expired evidence for report {}", report.getId(), failure);
                    }
                }));
    }
}
