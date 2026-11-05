package com.frozenheart.backend.modules.auth.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.security.JwtService;
import com.frozenheart.backend.core.util.CookieUtils;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.ConfirmEmailVerificationRequest;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        User user = User.builder().id(21L).email("student@sqb.edu.vn").passwordHash("old-hash").build();
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
    void confirmEmailVerificationUsesCurrentJwtUser() {
        setCurrentUser(22L, "student@sqb.edu.vn");
        User user = User.builder().id(22L).email("student@sqb.edu.vn").verified(false).build();

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:student@sqb.edu.vn")).thenReturn("123456");

        service.confirmEmailVerification(new ConfirmEmailVerificationRequest("123456"));

        assertTrue(user.isVerified());
        verify(userRepository).save(user);
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
