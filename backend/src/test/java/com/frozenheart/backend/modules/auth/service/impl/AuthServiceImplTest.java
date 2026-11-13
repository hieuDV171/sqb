package com.frozenheart.backend.modules.auth.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserDevice;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.DevicePlatform;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.security.AccountState;
import com.frozenheart.backend.core.security.JwtService;
import com.frozenheart.backend.core.security.TokenType;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.CookieUtils;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.ConfirmEmailVerificationRequest;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.service.EmailService;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock CookieUtils cookieUtils;
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserGamificationRepository userGamificationRepository;
    @Mock UserPushSettingRepository userPushSettingRepository;
    @Mock UserDeviceRepository userDeviceRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;
    @Mock MetricService metricService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AuthServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePasswordMarksFirstLoginSetupCompleted() {
        setCurrentUser(21L, "student@sqb.edu.vn");
        User user = User.builder().id(21L).email("student@sqb.edu.vn").passwordHash("old-hash").verified(true).build();
        UserProfile profile = UserProfile.builder().user(user).profileCompleted(false).build();

        when(userRepository.findById(21L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("temporary", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("private-password")).thenReturn("new-hash");

        service.changePassword(new ChangePasswordRequest("temporary", "private-password"));

        assertTrue(profile.isProfileCompleted());
        verify(userRepository).save(user);
        verify(userProfileRepository).save(profile);
    }

    @Test
    void loginForIncompleteAccountReturnsOnboardingTokenWithoutRefreshToken() {
        User user = User.builder()
                .id(24L)
                .email("admin@sqb.edu.vn")
                .passwordHash("temporary-hash")
                .role(UserRole.ADMIN)
                .verified(false)
                .active(true)
                .build();
        UserProfile profile = UserProfile.builder().user(user).profileCompleted(false).build();
        LoginRequest request = new LoginRequest(
                user.getEmail(), "temporary", "web-device", "untrusted-fid", DevicePlatform.WEB,
                "Chrome", "Windows", "1.0.0"
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("temporary", "temporary-hash")).thenReturn(true);
        when(userProfileRepository.findByUserId(24L)).thenReturn(Optional.of(profile));
        when(userDeviceRepository.findByUserIdAndDeviceId(24L, "web-device")).thenReturn(Optional.empty());
        when(jwtService.generateOnboardingToken(org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.eq(user.getEmail())))
                .thenReturn("onboarding-token");

        AuthResponse response = service.login(request);

        assertEquals(TokenType.ONBOARDING, response.tokenType());
        assertEquals(AccountState.EMAIL_VERIFICATION_REQUIRED, response.accountState());
        assertEquals("onboarding-token", response.accessToken());
        assertEquals(null, response.refreshToken());
        verify(jwtService, never()).generateRefreshToken(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(cookieUtils).clearCookie("refreshToken");
        verify(userDeviceRepository, never()).lockFidForTransaction("untrusted-fid");
    }

    @Test
    void loginSucceedsWithoutBindingAnotherAccountsFidEvenWithMatchingDeviceId() {
        String email = "student@sqb.edu.vn";
        String deviceId = "shared-device-id";
        String fid = "another-accounts-fid";
        User user = User.builder().id(26L).email(email).passwordHash("hash")
                .role(UserRole.STUDENT).verified(true).active(true).build();
        UserProfile profile = UserProfile.builder().user(user).profileCompleted(true).build();
        UserDevice device = UserDevice.builder().user(user).deviceId(deviceId).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(26L)).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(userDeviceRepository.findByUserIdAndDeviceId(26L, deviceId)).thenReturn(Optional.of(device));
        when(userDeviceRepository.isFidBoundToAnotherDevice(fid, 26L, deviceId)).thenReturn(true);
        when(jwtService.generateAccessToken(anyMap(), eq(email))).thenReturn("access");
        when(jwtService.generateRefreshToken(eq(email), eq(26L), eq(deviceId),
                org.mockito.ArgumentMatchers.anyString())).thenReturn("refresh");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = service.login(new LoginRequest(
                email, "password", deviceId, fid, DevicePlatform.WEB, null, null, null));

        assertEquals(TokenType.ACCESS, response.tokenType());
        assertNull(device.getFid());
        verify(userDeviceRepository).save(device);
    }

    @Test
    void refreshTokenCannotBeUsedAfterItsEmailIsAssignedToAnotherAccount() {
        String email = "student@sqb.edu.vn";
        String token = "old-refresh-token";
        var payload = JwtPayload.builder().username(email).userId(26L).deviceId("browser")
                .tokenType(TokenType.REFRESH).build();
        var newAccount = User.builder().id(27L).email(email).build();
        when(cookieUtils.getCookieValue("refreshToken")).thenReturn(token);
        when(jwtService.decodeToken(token)).thenReturn(payload);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rt:" + email + ":browser")).thenReturn(token);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(newAccount));

        AppException exception = assertThrows(AppException.class, () -> service.refreshToken(null));

        assertEquals(ResponseCode.TOKEN_INVALID_OR_EXPIRED, exception.getResponseCode());
    }

    @Test
    void legacyRefreshTokenMustHaveBeenIssuedDuringTheCurrentAccountsLifetime() {
        String email = "student@sqb.edu.vn";
        String token = "legacy-refresh-token";
        var issuedAt = Instant.parse("2026-11-01T10:00:00Z");
        var payload = JwtPayload.builder().username(email).deviceId("browser").issuedAt(issuedAt)
                .tokenType(TokenType.REFRESH).build();
        var newAccount = User.builder().id(27L).email(email).createdAt(issuedAt.plusSeconds(1)).build();
        when(cookieUtils.getCookieValue("refreshToken")).thenReturn(token);
        when(jwtService.decodeToken(token)).thenReturn(payload);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rt:" + email + ":browser")).thenReturn(token);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(newAccount));

        AppException exception = assertThrows(AppException.class, () -> service.refreshToken(null));

        assertEquals(ResponseCode.TOKEN_INVALID_OR_EXPIRED, exception.getResponseCode());
    }

    @Test
    void changePasswordRejectsUnverifiedFirstLoginAccount() {
        setCurrentUser(23L, "unverified@sqb.edu.vn");
        User user = User.builder().id(23L).email("unverified@sqb.edu.vn").verified(false).build();
        UserProfile profile = UserProfile.builder().user(user).profileCompleted(false).build();

        when(userRepository.findById(23L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(23L)).thenReturn(Optional.of(profile));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.changePassword(new ChangePasswordRequest("temporary", "private-password"))
        );

        assertEquals(ResponseCode.ACCOUNT_ONBOARDING_REQUIRED, exception.getResponseCode());
    }

    @Test
    void confirmEmailVerificationUsesCurrentJwtUser() {
        setCurrentUser(22L, "student@sqb.edu.vn");
        User user = User.builder().id(22L).email("student@sqb.edu.vn").verified(false).build();

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:student@sqb.edu.vn")).thenReturn("123456");

        service.confirmEmailVerification(new ConfirmEmailVerificationRequest("123456"));

        assertTrue(user.isVerified());
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.USER, user.getId()));
        verify(redisTemplate).delete("otp:student@sqb.edu.vn");
    }

    @Test
    void confirmEmailVerificationInvalidatesOtpAfterFifthWrongAttempt() {
        setCurrentUser(25L, "student@sqb.edu.vn");
        User user = User.builder().id(25L).email("student@sqb.edu.vn").verified(false).build();

        when(userRepository.findById(25L)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:student@sqb.edu.vn")).thenReturn("123456");
        when(valueOperations.increment("otp_attempt:student@sqb.edu.vn")).thenReturn(5L);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.confirmEmailVerification(new ConfirmEmailVerificationRequest("000000"))
        );

        assertEquals(ResponseCode.OTP_ATTEMPT_LIMIT_EXCEEDED, exception.getResponseCode());
        verify(redisTemplate).delete("otp:student@sqb.edu.vn");
    }

    private void setCurrentUser(Long userId, String email) {
        JwtPayload payload = JwtPayload.builder().userId(userId).username(email).role("STUDENT").deviceId("web").build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, java.util.List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
