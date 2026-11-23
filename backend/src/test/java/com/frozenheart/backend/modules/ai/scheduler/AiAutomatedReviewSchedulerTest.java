package com.frozenheart.backend.modules.ai.scheduler;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.core.entity.session.DuplicateCheckStatus;
import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock NotificationWriter notificationWriter;
    @Mock CounterMetricsService counterMetricsService;
    @Mock GamificationService gamificationService;
    @Mock ActivityFeedService activityFeedService;
    @Mock AnonymizerUtil anonymizerUtil;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AiAutomatedReviewScheduler scheduler;

    @Test
    void pagesIdsBeforeBatchFetchingTheEntityGraph() {
        when(sessionRepository.findReadyForAutoReview(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(11L, 12L));
        when(sessionRepository.lockReviewSessions(List.of(11L, 12L), true)).thenReturn(List.of(12L));

        scheduler.runAutomatedFallbackReviewWorkflow();

        verify(sessionRepository).findReadyForAutoReview(any(Instant.class), any(Pageable.class));
        verify(sessionRepository).findByIdInFetchAutomatedReviewDetails(List.of(12L));
    }

    @Test
    void automatedApprovalOnlyKeepsDifficultyThatWasAlreadyApproved() {
        Question pending = Question.builder().id(21L).status(QuestionStatus.PENDING).duplicateCheckStatus(DuplicateCheckStatus.COMPLETE).difficulty(QuestionDifficulty.EASY).build();
        Question approved = Question.builder().id(22L).status(QuestionStatus.APPROVED).difficulty(QuestionDifficulty.HARD).build();
        Session session = Session.builder().id(12L).status(SessionStatus.PENDING)
                .questions(new java.util.LinkedHashSet<>(List.of(pending, approved))).build();
        when(sessionRepository.findReadyForAutoReview(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(12L));
        when(sessionRepository.lockReviewSessions(List.of(12L), true)).thenReturn(List.of(12L));
        when(sessionRepository.findByIdInFetchAutomatedReviewDetails(List.of(12L))).thenReturn(List.of(session));

        scheduler.runAutomatedFallbackReviewWorkflow();

        assertEquals(QuestionStatus.APPROVED, pending.getStatus());
        assertEquals(QuestionDifficulty.UNCLASSIFIED, pending.getDifficulty());
        assertEquals(QuestionDifficulty.HARD, approved.getDifficulty());
        verify(questionRepository).saveAll(List.of(pending));
    }

    @Test
    void daySixDecidesCleanAndExactButLeavesPendingTechnicalFailures() {
        Question clean = Question.builder().id(31L).status(QuestionStatus.PENDING)
                .duplicateCheckStatus(DuplicateCheckStatus.COMPLETE).build();
        Question duplicate = Question.builder().id(32L).status(QuestionStatus.PENDING)
                .duplicateCheckStatus(DuplicateCheckStatus.COMPLETE)
                .duplicateWarnings(List.of(DuplicateWarning.builder().kind(DuplicateWarning.Kind.EXACT).build())).build();
        Question failed = Question.builder().id(33L).status(QuestionStatus.PENDING)
                .duplicateCheckStatus(DuplicateCheckStatus.RETRY).build();
        Session session = Session.builder().id(12L).status(SessionStatus.PENDING)
                .questions(new java.util.LinkedHashSet<>(List.of(clean, duplicate, failed))).build();
        when(sessionRepository.findReadyForAutoReview(any(Instant.class), any(Pageable.class))).thenReturn(List.of(12L));
        when(sessionRepository.lockReviewSessions(List.of(12L), true)).thenReturn(List.of(12L));
        when(sessionRepository.findByIdInFetchAutomatedReviewDetails(List.of(12L))).thenReturn(List.of(session));

        scheduler.runAutomatedFallbackReviewWorkflow();

        assertEquals(QuestionStatus.APPROVED, clean.getStatus());
        assertEquals(QuestionStatus.REJECTED, duplicate.getStatus());
        assertEquals(QuestionStatus.PENDING, failed.getStatus());
        assertEquals(SessionStatus.PENDING, session.getStatus());
        verify(questionRepository).saveAll(List.of(clean, duplicate));
        org.mockito.Mockito.verifyNoInteractions(gamificationService, counterMetricsService);
    }
}
