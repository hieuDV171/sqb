package com.frozenheart.backend.modules.badge;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;
import com.frozenheart.backend.core.entity.badge.criteria.CounterThresholdCriteria;
import com.frozenheart.backend.core.entity.badge.criteria.LeaderboardRankCriteria;
import com.frozenheart.backend.core.entity.badge.criteria.StreakCriteria;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.badge.dto.CreateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.UpdateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.evaluator.CounterThresholdEvaluator;
import com.frozenheart.backend.modules.badge.evaluator.LeaderboardRankEvaluator;
import com.frozenheart.backend.modules.badge.evaluator.StreakEvaluator;
import com.frozenheart.backend.modules.badge.repository.BadgeRepository;
import com.frozenheart.backend.modules.badge.repository.UserBadgeRepository;
import com.frozenheart.backend.modules.badge.service.BadgeEvaluationService;
import com.frozenheart.backend.modules.badge.service.impl.BadgeServiceImpl;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.gamification.dto.LeaderboardPeriod;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BadgeConfigurationTest {
    private final BadgeRepository badges = mock(BadgeRepository.class);
    private final UserBadgeRepository awards = mock(UserBadgeRepository.class);
    private final NotificationWriter notifications = mock(NotificationWriter.class);
    private final BadgeServiceImpl service = new BadgeServiceImpl(badges, awards, mock(UserRepository.class),
            notifications, new BadgeEvaluationService(List.of(new StreakEvaluator())), mock(UserProfileRepository.class),
            mock(ActivityFeedService.class), mock(MediaService.class), mock(ApplicationEventPublisher.class),
            mock(SubjectRepository.class));

    @Test
    void usesSavedThresholdOnNextEventWithoutAwardingOnCreate() {
        when(badges.save(any(Badge.class))).thenAnswer(invocation -> {
            Badge badge = invocation.getArgument(0);
            badge.setId(42L);
            return badge;
        });
        var saved = service.createBadge(CreateBadgeRequestDto.builder().name("Chuỗi mới")
                .badgeTriggerEvent(BadgeTriggerEvent.STUDY_STREAK).criteria(new StreakCriteria(7)).build());
        verify(awards, never()).insertAutomaticAward(any(), any(), any());

        var badge = Badge.builder().id(saved.getBadgeId()).name(saved.getName()).active(saved.getActive())
                .badgeTriggerEvent(saved.getBadgeTriggerEvent()).criteria(saved.getCriteria()).build();
        var user = User.builder().id(1L).build();
        when(badges.findUnearnedActiveBadgesByUserAndBadgeTriggerEvent(1L, BadgeTriggerEvent.STUDY_STREAK))
                .thenReturn(List.of(badge));
        when(awards.insertAutomaticAward(eq(1L), eq(42L), any())).thenReturn(1);

        service.checkAndGrantBadges(user, BadgeTriggerEvent.STUDY_STREAK, 6);
        verify(awards, never()).insertAutomaticAward(any(), any(), any());
        service.checkAndGrantBadges(user, BadgeTriggerEvent.STUDY_STREAK, 7);
        verify(awards).insertAutomaticAward(eq(1L), eq(42L), any());
        verify(notifications).create(any());
    }

    @Test
    void rejectsMismatchedPartialUpdateAndInvalidThresholds() {
        var badge = Badge.builder().id(42L).badgeTriggerEvent(BadgeTriggerEvent.STUDY_STREAK)
                .criteria(new StreakCriteria(7)).build();
        when(badges.findByIdForGrant(42L)).thenReturn(Optional.of(badge));

        assertThatThrownBy(() -> service.updateBadge(42L, UpdateBadgeRequestDto.builder()
                .badgeTriggerEvent(BadgeTriggerEvent.APPROVED_QUESTIONS).build()))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.INVALID_PARAMETER_VALUE));
        assertThatThrownBy(() -> service.updateBadge(42L, UpdateBadgeRequestDto.builder()
                .criteria(new StreakCriteria(0)).build())).isInstanceOf(AppException.class);
        assertThat(badge.getBadgeTriggerEvent()).isEqualTo(BadgeTriggerEvent.STUDY_STREAK);
        verify(badges, never()).save(any());
    }

    @Test
    void subjectRankCannotBeSavedOrGrantedFromSemesterResults() {
        var subjectRank = new LeaderboardRankCriteria(1, LeaderboardPeriod.SUBJECT);
        var outsideTopTen = new LeaderboardRankCriteria(11, LeaderboardPeriod.SEMESTER);
        var request = CreateBadgeRequestDto.builder().name("Thứ hạng")
                .badgeTriggerEvent(BadgeTriggerEvent.LEADERBOARD_RANK);

        assertThatThrownBy(() -> service.createBadge(request.criteria(subjectRank).build())).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.createBadge(request.criteria(outsideTopTen).build())).isInstanceOf(AppException.class);
        assertThat(new LeaderboardRankEvaluator().evaluate(User.builder().id(1L).build(), null, subjectRank, 1)).isFalse();
    }

    @Test
    void reviewedSessionsCountsOnlyResolvedSessions() {
        var profiles = mock(UserProfileRepository.class);
        var sessions = mock(SessionRepository.class);
        var evaluator = new CounterThresholdEvaluator(profiles, mock(UserGamificationRepository.class),
                mock(FriendshipRepository.class), sessions);
        var badge = Badge.builder().badgeTriggerEvent(BadgeTriggerEvent.REVIEWED_SESSIONS).build();
        var user = User.builder().id(1L).build();
        when(sessions.countByReviewerIdAndStatus(1L, SessionStatus.RESOLVED)).thenReturn(2);

        assertThat(evaluator.evaluate(user, badge, CounterThresholdCriteria.builder().threshold(3).build(), null)).isFalse();
        assertThat(evaluator.evaluate(user, badge, CounterThresholdCriteria.builder().threshold(2).build(), null)).isTrue();
        verify(profiles, never()).findById(any());
    }
}
