package com.frozenheart.backend.modules.gamification.scheduler;

import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.gamification.service.impl.Game3ResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationScheduler {

    private static final LocalTime GAME_1_BATCH_DEADLINE = LocalTime.of(0, 20);

    private final GamificationService gamificationService;
    private final Game3ResolutionService game3ResolutionService;

    /**
     * Chốt Game 1 lúc 00:05. Không bắt đầu lô mới từ 00:20 theo giờ Việt Nam.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = Time.DEFAULT_TIMEZONE)
    public void scheduleDailyGame1Resolution() {
        ZoneId schoolZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        Instant deadline = LocalDate.now(schoolZone).atTime(GAME_1_BATCH_DEADLINE).atZone(schoolZone).toInstant();
        resolveGame1BatchesUntil(deadline);
    }

    void resolveGame1BatchesUntil(Instant deadline) {
        long resolved = 0;
        log.info("[GamificationScheduler] Starting Game 1 resolution, batch deadline {}", deadline);
        try {
            while (Instant.now().isBefore(deadline)) {
                // The scheduler has no transaction. Each service invocation commits its own batch.
                int batchSize = gamificationService.resolveNextGame1Batch();
                if (batchSize == 0) {
                    log.info("[GamificationScheduler] Game 1 resolution complete, resolved {} predictions", resolved);
                    return;
                }
                resolved += batchSize;
            }
            log.warn("[GamificationScheduler] Game 1 batch deadline reached after resolving {} predictions; any remaining predictions will be retried next night", resolved);
        } catch (Exception e) {
            log.error("[GamificationScheduler] Game 1 resolution stopped after committing {} predictions", resolved, e);
        }
    }

    @Scheduled(cron = "0 30 0 * * *", zone = Time.DEFAULT_TIMEZONE)
    public void scheduleDailyGame3Resolution() {
        Instant cutoff = Instant.now();
        int processed = 0;
        try {
            int batchSize;
            while ((batchSize = game3ResolutionService.resolveNextDueBatch(cutoff)) > 0) {
                processed += batchSize;
            }
            log.info("[GamificationScheduler] Processed {} Game 3 evaluations", processed);
        } catch (Exception e) {
            log.error("[GamificationScheduler] Game 3 stopped after committing {} evaluations", processed, e);
        }
    }

    /**
     * CronJob chạy hàng tuần vào 01:00 AM Thứ 7 để tổng kết phiên cũ và mở phiên Game 6 mới cho ngày Thứ 7.
     */
    @Scheduled(cron = "0 0 1 * * SAT", zone = Time.DEFAULT_TIMEZONE)
    public void scheduleWeeklyGame6Process() {
        log.info("[GamificationScheduler] Triggering weekly Game 6 resolution and question generation...");
        try {
            gamificationService.processAndGenerateGame6WeeklySession();
        } catch (Exception e) {
            log.error("[GamificationScheduler] Error executing weekly Game 6 resolution and generation", e);
        }
    }

    /**
     * CronJob chạy vào 07:00 AM ngày đầu tiên mỗi tháng để vinh danh Top 3 SV dẫn đầu học kỳ.
     */
    @Scheduled(cron = "0 0 7 1 * *", zone = Time.DEFAULT_TIMEZONE)
    public void scheduleMonthlyLeaderboardHonorPost() {
        log.info("[GamificationScheduler] Triggering monthly leaderboard honor post...");
        try {
            gamificationService.publishMonthlyLeaderboardHonorPost();
        } catch (Exception e) {
            log.error("[GamificationScheduler] Error executing monthly leaderboard honor post", e);
        }
    }
}
