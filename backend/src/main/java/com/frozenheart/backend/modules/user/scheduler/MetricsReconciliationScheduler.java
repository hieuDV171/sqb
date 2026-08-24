package com.frozenheart.backend.modules.user.scheduler;

import org.springframework.stereotype.Component;

import com.frozenheart.backend.modules.user.service.CounterMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsReconciliationScheduler {

    private final CounterMetricsService counterMetricsService;

    /**
     * Periodic background job running every Sunday at 3:00 AM
     * Re-calculates and aligns all denormalized counter fields against source table aggregates.
     */

    
    // @Scheduled(cron = "0 0 3 * * SUN")
    public void runWeeklyMetricsReconciliation() {
        log.info("Triggered weekly metric reconciliation scheduler task.");
        try {
            counterMetricsService.reconcileAllUserProfileCounters();
        } catch (Exception e) {
            log.error("Failed to reconcile user profile counter metrics: ", e);
        }
    }
}
