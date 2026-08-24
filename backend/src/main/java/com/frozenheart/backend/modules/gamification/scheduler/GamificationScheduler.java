package com.frozenheart.backend.modules.gamification.scheduler;

import com.frozenheart.backend.modules.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationScheduler {

    private final GamificationService gamificationService;

    /**
     * CronJob chạy hàng ngày lúc 00:05 để tổng kết Game 1 cho ngày hôm qua.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void scheduleDailyGame1Resolution() {
        log.info("[GamificationScheduler] Triggering daily Game 1 resolution...");
        try {
            gamificationService.resolveGame1Daily();
        } catch (Exception e) {
            log.error("[GamificationScheduler] Error executing daily Game 1 resolution", e);
        }
    }
}
