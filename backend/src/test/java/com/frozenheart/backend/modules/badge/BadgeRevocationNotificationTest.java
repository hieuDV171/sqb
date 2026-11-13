package com.frozenheart.backend.modules.badge;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.notification.NotificationCategory;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.badge.repository.BadgeRepository;
import com.frozenheart.backend.modules.badge.repository.UserBadgeRepository;
import com.frozenheart.backend.modules.badge.service.BadgeEvaluationService;
import com.frozenheart.backend.modules.badge.service.impl.BadgeServiceImpl;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeRevocationNotificationTest {
    @Captor
    private ArgumentCaptor<List<Notification>> notifications;

    @Test
    void notifiesOnlyActiveRecipientsWhoseBadgeWasRevokedAndSkipsRepeatedRequests() {
        var badges = mock(BadgeRepository.class);
        var awards = mock(UserBadgeRepository.class);
        var users = mock(UserRepository.class);
        var writer = mock(NotificationWriter.class);
        var profiles = mock(UserProfileRepository.class);
        var service = new BadgeServiceImpl(badges, awards, users, writer, mock(BadgeEvaluationService.class), profiles,
                mock(ActivityFeedService.class), mock(MediaService.class), mock(ApplicationEventPublisher.class),
                mock(SubjectRepository.class));
        var badge = Badge.builder().id(4L).name("Cây bút Vàng").build();
        var request = List.of(1L, 1L, 2L, 3L);
        var affected = Set.of(1L, 2L);
        when(badges.findByIdForGrant(4L)).thenReturn(Optional.of(badge));
        when(awards.findCurrentUserIds(4L, request)).thenReturn(affected, Set.of());
        when(users.findAllById(affected)).thenReturn(List.of(User.builder().id(1L).active(true).build(),
                User.builder().id(2L).active(false).build()));

        service.revokeBadge(4L, request);
        service.revokeBadge(4L, request);

        verify(profiles).decrementBadgesCountForUsers(affected);
        verify(writer).createAll(notifications.capture());
        verifyNoMoreInteractions(writer);
        assertThat(notifications.getValue()).singleElement().satisfies(notification -> {
            assertThat(notification.getReceiver().getId()).isEqualTo(1L);
            assertThat(notification.getType()).isEqualTo(NotificationType.BADGE_REVOKED);
            assertThat(notification.getCategory()).isEqualTo(NotificationCategory.GAMIFICATION);
            assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.BADGE);
            assertThat(notification.getTargetId()).isEqualTo(4L);
            assertThat(notification.getTitle()).contains("Cây bút Vàng");
        });
    }
}
