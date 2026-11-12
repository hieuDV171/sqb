package com.frozenheart.backend.modules.gamification.scheduler;

import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GamificationSchedulerTest {
    private final GamificationService service = mock(GamificationService.class);
    private final GamificationScheduler scheduler = new GamificationScheduler(service);

    @Test
    void startsOncePerDayAtFivePastMidnightInVietnam() throws NoSuchMethodException {
        var annotation = GamificationScheduler.class.getMethod("scheduleDailyGame1Resolution")
                .getAnnotation(Scheduled.class);
        assertEquals(Time.DEFAULT_TIMEZONE, annotation.zone());
        var cron = CronExpression.parse(annotation.cron());
        var midnight = LocalDate.of(2026, 11, 12).atStartOfDay(ZoneId.of(annotation.zone()));

        var firstRun = midnight.plusMinutes(5);
        assertEquals(firstRun, cron.next(midnight));
        assertEquals(firstRun.plusDays(1), cron.next(firstRun));
    }

    @Test
    void continuesAcrossDaysEvenWhenOneBatchHasFewerThanFiveHundredPredictions() {
        when(service.resolveNextGame1Batch()).thenReturn(200, 500, 300, 0);

        scheduler.resolveGame1BatchesUntil(Instant.now().plusSeconds(60));

        verify(service, times(4)).resolveNextGame1Batch();
    }

    @Test
    void doesNotStartABatchAfterTheDeadline() {
        scheduler.resolveGame1BatchesUntil(Instant.now().minusSeconds(1));

        verifyNoInteractions(service);
    }
}
