package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.event.AccountDeletedEvent;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.dto.event.MediaCleanupEvent;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserGamification;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final UserGamificationRepository gamification = mock(UserGamificationRepository.class);
    private final UserDeviceRepository devices = mock(UserDeviceRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AccountDeletionService service = new AccountDeletionService(users, profiles, gamification, devices,
            encoder, events);

    @Test
    void schedulesOnlyOnceAndKeepsAccountActive() {
        User user = activeUser();
        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var request = service.request(1L);

        assertThat(request.scheduledFor()).isBetween(before.plus(30, ChronoUnit.DAYS),
                Instant.now().plus(30, ChronoUnit.DAYS));
        assertThat(request.canCancel()).isTrue();
        assertThat(service.request(1L)).isEqualTo(request);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertThat(service.getStatus(1L)).isEqualTo(request);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getEmail()).isEqualTo("student@example.edu");
        verifyNoInteractions(profiles, gamification, devices, encoder, events);
    }

    @Test
    void cancelsBeforeDeadlineAndDoesNothingIfCalledAgain() {
        User user = activeUser();
        service.request(1L);

        assertThat(service.cancel(1L).scheduledFor()).isNull();
        assertThat(service.cancel(1L).canCancel()).isFalse();
        assertThat(user.getDeletionDueAt()).isNull();
        assertThat(user.isActive()).isTrue();
        service.processDueAccounts(Instant.now());
        verifyNoInteractions(profiles, gamification, devices, encoder, events);
    }

    @Test
    void rejectsCancellationAfterDeadlineEvenIfSchedulerHasNotRun() {
        User user = activeUser();
        Instant deadline = Instant.now().minusSeconds(1);
        user.setDeletionDueAt(deadline);

        assertThatExceptionOfType(AppException.class).isThrownBy(() -> service.cancel(1L))
                .satisfies(error -> assertThat(error.getResponseCode())
                        .isEqualTo(ResponseCode.ACCOUNT_DELETION_CANCELLATION_EXPIRED));
        assertThat(user.getDeletionDueAt()).isEqualTo(deadline);
        assertThat(service.request(1L).canCancel()).isFalse();
    }

    @Test
    void anonymizesDueAccountsInOneBatchAndPublishesCleanup() {
        User user = activeUser();
        Instant deadline = Instant.now();
        user.setDeletionDueAt(deadline);
        var profile = UserProfile.builder().userId(1L).fullName("Student").className("Class")
                .dateOfBirth(LocalDate.of(2000, 1, 1)).avatarUrl("avatar.jpg").profileCompleted(true).build();
        var points = UserGamification.builder().userId(1L).publicPoints(10).coinBalance(8).build();
        when(users.findDueAccountsForDeletion(eq(deadline), any(Pageable.class))).thenReturn(List.of(user));
        when(profiles.findAllById(List.of(1L))).thenReturn(List.of(profile));
        when(gamification.findAllById(List.of(1L))).thenReturn(List.of(points));
        when(encoder.encode(anyString())).thenReturn("disabled-password");

        service.processDueAccounts(deadline);

        assertThat(user.isActive()).isFalse();
        assertThat(user.isVerified()).isFalse();
        assertThat(user.getDeletionDueAt()).isNull();
        assertThat(user.getEmail()).startsWith("Deleted_Account_");
        assertThat(user.getPasswordHash()).isEqualTo("disabled-password");
        assertThat(profile.getFullName()).isEqualTo("Tài khoản đã bị xóa");
        assertThat(profile.getDateOfBirth()).isNull();
        assertThat(profile.getClassName()).isEmpty();
        assertThat(profile.getAvatarUrl()).isEmpty();
        assertThat(points.getPublicPoints()).isZero();
        assertThat(points.getCoinBalance()).isZero();
        verify(devices).deactivateForAccounts(List.of(1L));
        verify(events).publishEvent(new AccountDeletedEvent(List.of(1L)));
        verify(events).publishEvent(any(EntitySearchSyncEvent.class));
        verify(events).publishEvent(any(MediaCleanupEvent.class));
    }

    private User activeUser() {
        User user = User.builder().id(1L).email("student@example.edu").active(true).verified(true).build();
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        return user;
    }
}
