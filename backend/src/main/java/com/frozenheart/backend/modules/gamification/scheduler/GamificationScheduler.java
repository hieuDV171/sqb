package com.frozenheart.backend.modules.gamification.scheduler;

import com.frozenheart.backend.core.constant.Time;
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
    @Scheduled(cron = "0 5 0 * * *", zone = Time.DEFAULT_TIMEZONE)
    public void scheduleDailyGame1Resolution() {
        log.info("[GamificationScheduler] Triggering daily Game 1 resolution...");
        try {
            gamificationService.resolveGame1Daily();
        } catch (Exception e) {
            log.error("[GamificationScheduler] Error executing daily Game 1 resolution", e);
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
