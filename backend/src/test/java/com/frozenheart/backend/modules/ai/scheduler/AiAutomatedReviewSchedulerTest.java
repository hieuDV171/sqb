package com.frozenheart.backend.modules.ai.scheduler;

import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.ai.service.AiIntegrationService;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAutomatedReviewSchedulerTest {

    @Mock SessionRepository sessionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock PostRepository postRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock AiIntegrationService aiIntegrationService;
    @Mock CounterMetricsService counterMetricsService;
    @Mock GamificationService gamificationService;
    @Mock ActivityFeedService activityFeedService;
    @Mock AnonymizerUtil anonymizerUtil;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AiAutomatedReviewScheduler scheduler;

    @Test
    void pagesIdsBeforeBatchFetchingTheEntityGraph() {
        when(sessionRepository.findIdsByStatusAndCreatedAtBefore(
                eq(SessionStatus.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(11L, 12L));
        when(sessionRepository.findByIdInFetchAutomatedReviewDetails(List.of(11L, 12L)))
                .thenReturn(List.of());
        when(sessionRepository.lockReviewSessions(List.of(11L, 12L), true)).thenReturn(List.of(12L));

        scheduler.runAutomatedFallbackReviewWorkflow();

        verify(sessionRepository, times(2)).findIdsByStatusAndCreatedAtBefore(
                eq(SessionStatus.PENDING), any(Instant.class), any(Pageable.class));
        verify(sessionRepository).findByIdInFetchAutomatedReviewDetails(List.of(11L, 12L));
        verify(sessionRepository).findByIdInFetchAutomatedReviewDetails(List.of(12L));
    }
}
