package com.frozenheart.backend.modules.auth.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.*;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.*;
import com.frozenheart.backend.modules.auth.dto.admin.AdminResetPasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.user.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthPasswordFlowsTest {
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserGamificationRepository userGamificationRepository;
    @Mock UserPushSettingRepository userPushSettingRepository;
    @Mock UserDeviceRepository userDeviceRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AuthServiceImpl service;

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void resetReturnsOnlyNewCredentialAndRevokesOldTokensWithoutReverifyingEmail() {
        var user = User.builder().id(4L).email("student@example.edu").role(UserRole.STUDENT)
                .verified(true).active(true).sessionEpoch(3L).passwordHash("old").build();
        var profile = UserProfile.builder().user(user).fullName("Sinh viên")
                .studentLecturerCode("00123").profileCompleted(true).build();
        when(userRepository.findByEmailForLogin(user.getEmail())).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(4L)).thenReturn(Optional.of(profile));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        var result = service.adminResetPassword(new AdminResetPasswordRequest(user.getEmail(), null));

        assertThat(result.userId()).isEqualTo(4L);
        assertThat(result.temporaryPassword()).hasSize(16);
        assertThat(PasswordPolicy.requireValid(result.temporaryPassword())).isEqualTo(result.temporaryPassword());
        verify(passwordEncoder).encode(result.temporaryPassword());
        assertThat(result.toString()).doesNotContain(result.temporaryPassword());
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(profile.isProfileCompleted()).isFalse();
        assertThat(user.isVerified()).isTrue();
        assertThat(user.getSessionEpoch()).isEqualTo(4L);
        assertThat(AccountState.from(user.isVerified(), profile.isProfileCompleted())).isEqualTo(AccountState.PASSWORD_CHANGE_REQUIRED);
        var jwt = new JwtService(null);
        assertThat(jwt.isTokenValid(JwtPayload.builder().userId(4L).username(user.getEmail()).sessionEpoch(3L).build(), new CustomUserDetails(user))).isFalse();
        verify(userDeviceRepository).deactivateForAccounts(List.of(4L));
        verify(eventPublisher).publishEvent(new AccountWebSocketSessions.CredentialsChanged(4L));
    }

    @Test void invalidCustomResetDoesNotChangeHashProfileOrEpoch() {
        var user = User.builder().id(4L).email("student@example.edu").passwordHash("old").build();
        var profile = UserProfile.builder().profileCompleted(true).build();
        when(userRepository.findByEmailForLogin(user.getEmail())).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(4L)).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> service.adminResetPassword(new AdminResetPasswordRequest(user.getEmail(), "weak"))).isInstanceOf(AppException.class);
        assertThat(user.getPasswordHash()).isEqualTo("old");
        assertThat(user.getSessionEpoch()).isNull();
        assertThat(profile.isProfileCompleted()).isTrue();
        verifyNoInteractions(passwordEncoder, userDeviceRepository, eventPublisher);
        verify(userRepository, never()).save(any());
    }

    @Test void customResetPreservesSpacesAndInitialEpochAdvancesToOne() {
        var user = User.builder().id(4L).email("student@example.edu").build();
        var profile = UserProfile.builder().profileCompleted(true).build();
        when(userRepository.findByEmailForLogin(user.getEmail())).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(4L)).thenReturn(Optional.of(profile));
        var result = service.adminResetPassword(new AdminResetPasswordRequest(user.getEmail(), "  Abcdef1234!  "));
        assertThat(result.temporaryPassword()).isEqualTo("  Abcdef1234!  ");
        assertThat(user.getSessionEpoch()).isEqualTo(1L);
    }

    @Test void bulkReturnsCredentialsOnlyForSuccessfulRowsAndNeverResetsExistingAccounts() {
        when(userRepository.findExistingEmailsByEmailIn(any())).thenReturn(Set.of("old@example.edu"));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.saveAll(anyList())).thenAnswer(call -> {
            List<User> users = call.getArgument(0);
            for (int i = 0; i < users.size(); i++) users.get(i).setId(10L + i);
            return users;
        });
        var result = service.bulkImportUsers(new BulkImportRequest(List.of(
                draft("new@example.edu", null), draft("custom@example.edu", "  Abcdef1234!  "),
                draft("old@example.edu", null), draft("weak@example.edu", "weak"))));
        assertThat(result.totalSuccess()).isEqualTo(2);
        assertThat(result.totalFailed()).isEqualTo(2);
        assertThat(result.credentials()).extracting(item -> item.email()).containsExactly("new@example.edu", "custom@example.edu");
        assertThat(result.credentials().get(0).temporaryPassword()).hasSize(16);
        assertThat(result.credentials().get(1).temporaryPassword()).isEqualTo("  Abcdef1234!  ");
        for (var credential : result.credentials()) verify(passwordEncoder).encode(credential.temporaryPassword());
        verify(userRepository, never()).findByEmailForLogin(anyString());
    }

    @Test void passwordChangeRejectsReuseEvenWhenPolicyIsSatisfied() {
        var user = User.builder().id(4L).verified(true).passwordHash("old").build();
        var profile = UserProfile.builder().profileCompleted(true).build();
        var auth = new UsernamePasswordAuthenticationToken("user", null, List.of());
        auth.setDetails(JwtPayload.builder().userId(4L).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(4L)).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("Abcdef1234!AA", "old")).thenReturn(true);
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("Abcdef1234!AA", "Abcdef1234!AA"))).isInstanceOf(AppException.class);
        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(eventPublisher, userDeviceRepository);
    }

    private SingleUserImportDto draft(String email, String password) {
        return new SingleUserImportDto(email, password, "Sinh viên", null, null, null, null, null, null, null, null);
    }

    @Test void passwordChangeRechecksEpochAfterWaitingForUserLock() {
        var auth = new UsernamePasswordAuthenticationToken("user", null, List.of());
        auth.setDetails(JwtPayload.builder().userId(4L).sessionEpoch(1L).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
        var user = User.builder().id(4L).sessionEpoch(2L).build();
        when(userRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("old", "NewSecurePass@2026")))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(passwordEncoder, userProfileRepository, userDeviceRepository, eventPublisher);
    }
}
