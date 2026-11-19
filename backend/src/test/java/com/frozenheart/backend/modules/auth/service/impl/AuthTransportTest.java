package com.frozenheart.backend.modules.auth.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.*;
import com.frozenheart.backend.core.util.CookieUtils;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTransportTest {
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserDeviceRepository userDeviceRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> values;
    @Mock JwtService jwtService;
    @Mock CookieUtils cookieUtils;
    @Mock MetricService metricService;
    @InjectMocks AuthServiceImpl service;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void webRejectsNativePlatformBeforeCheckingCredentials() {
        assertCode(ResponseCode.INVALID_PARAMETER_VALUE, () -> service.login(login(DevicePlatform.ANDROID)));
        verifyNoInteractions(userRepository, jwtService, cookieUtils);
    }

    @Test void nativeRejectsWebPlatform() {
        assertCode(ResponseCode.INVALID_PARAMETER_VALUE, () -> service.loginNative(login(DevicePlatform.WEB)));
        verifyNoInteractions(userRepository, jwtService, cookieUtils);
    }

    @Test void webLoginOnlyExposesAccessTokenAndSetsHttpOnlyRefreshCookie() {
        prepareLogin(AuthClientType.WEB);
        var response = service.login(login(DevicePlatform.WEB));
        assertEquals("access", response.accessToken());
        assertNull(response.refreshToken());
        verify(cookieUtils).addHttpOnlyCookie(eq("refreshToken"), eq("new-refresh"), any(Duration.class));
        verify(jwtService).generateAccessToken(argThat(c -> "WEB".equals(c.get("authClientType"))), eq("fixture@sqb.test"));
    }

    @Test void nativeLoginReturnsBothTokensWithoutReadingOrWritingCookies() {
        prepareLogin(AuthClientType.NATIVE);
        var response = service.loginNative(login(DevicePlatform.ANDROID));
        assertEquals("access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        verifyNoInteractions(cookieUtils);
    }

    @Test void webRefreshDoesNotFallBackToAJsonToken() {
        assertCode(ResponseCode.INVALID_PARAMETER_VALUE, () -> service.refreshToken(new RefreshTokenRequest("known-token")));
        verifyNoInteractions(cookieUtils, jwtService, redisTemplate);
    }

    @Test void webWithoutCookieCannotRefresh() {
        assertCode(ResponseCode.TOKEN_INVALID_OR_EXPIRED, () -> service.refreshToken(null));
        verifyNoInteractions(jwtService, redisTemplate);
    }

    @Test void webRefreshRejectsNativeCookie() {
        when(cookieUtils.getCookieValue("refreshToken")).thenReturn("old-refresh");
        when(jwtService.decodeToken("old-refresh")).thenReturn(payload(AuthClientType.NATIVE));
        assertCode(ResponseCode.TOKEN_INVALID_OR_EXPIRED, () -> service.refreshToken(null));
        verifyNoInteractions(redisTemplate, userRepository);
    }

    @Test void nativeRefreshRejectsWebTokenEvenIfCookieIsOmitted() {
        when(jwtService.decodeToken("old-refresh")).thenReturn(payload(AuthClientType.WEB));
        assertCode(ResponseCode.TOKEN_INVALID_OR_EXPIRED, () -> service.refreshNativeToken(new RefreshTokenRequest("old-refresh")));
        verifyNoInteractions(cookieUtils, redisTemplate, userRepository);
    }

    @Test void nativeRejectsUntypedLegacyRefreshToken() {
        when(jwtService.decodeToken("old-refresh")).thenReturn(payload(null));
        assertCode(ResponseCode.TOKEN_INVALID_OR_EXPIRED, () -> service.refreshNativeToken(new RefreshTokenRequest("old-refresh")));
        verifyNoInteractions(cookieUtils, redisTemplate);
    }

    @Test void nativeRefreshRotatesJsonTokensAndIgnoresCookies() {
        prepareRefresh(AuthClientType.NATIVE, AuthClientType.NATIVE, 1L);
        var response = service.refreshNativeToken(new RefreshTokenRequest("old-refresh"));
        assertEquals("access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        verifyNoInteractions(cookieUtils);
    }

    @Test void legacyWebCookieMigratesToTypedWebRefreshTokenWithoutJsonLeak() {
        prepareRefresh(null, AuthClientType.WEB, 1L);
        when(cookieUtils.getCookieValue("refreshToken")).thenReturn("old-refresh");
        var response = service.refreshToken(null);
        assertEquals("access", response.accessToken());
        assertNull(response.refreshToken());
        verify(cookieUtils).addHttpOnlyCookie(eq("refreshToken"), eq("new-refresh"), any(Duration.class));
    }

    @Test void rotationConflictDoesNotIssueNewTokensToTheCaller() {
        prepareRefresh(AuthClientType.NATIVE, AuthClientType.NATIVE, 0L);
        assertCode(ResponseCode.TOKEN_INVALID_OR_EXPIRED, () -> service.refreshNativeToken(new RefreshTokenRequest("old-refresh")));
        verifyNoInteractions(cookieUtils);
    }

    @Test void nativeLogoutRevokesRefreshWithoutTouchingWebCookie() {
        authenticate(payload(AuthClientType.NATIVE));
        service.logoutNative();
        verify(redisTemplate).delete("rt:fixture@sqb.test:device");
        verifyNoInteractions(cookieUtils);
    }

    @Test void webLogoutClearsCookieAndRejectsNativeAccessToken() {
        authenticate(payload(AuthClientType.NATIVE));
        assertCode(ResponseCode.TOKEN_INVALID_OR_EXPIRED, service::logout);
        verifyNoInteractions(redisTemplate, cookieUtils);
        authenticate(payload(AuthClientType.WEB));
        service.logout();
        verify(cookieUtils).clearCookie("refreshToken");
    }

    private LoginRequest login(DevicePlatform platform) {
        return new LoginRequest("fixture@sqb.test", "password", "device", null, platform, null, null, null);
    }

    private User user() {
        return User.builder().id(7L).email("fixture@sqb.test").role(UserRole.STUDENT)
                .passwordHash("hash").active(true).verified(true).build();
    }

    private JwtPayload payload(AuthClientType type) {
        return JwtPayload.builder().userId(7L).username("fixture@sqb.test").deviceId("device")
                .sessionId("session").tokenType(TokenType.REFRESH).authClientType(type).build();
    }

    private void prepareLogin(AuthClientType type) {
        var user = user();
        when(userRepository.findByEmailForLogin(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(userProfileRepository.findByUserId(7L)).thenReturn(Optional.of(UserProfile.builder().user(user).profileCompleted(true).build()));
        when(userDeviceRepository.findByUserIdAndDeviceId(7L, "device")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(values);
        tokens(type);
    }

    @SuppressWarnings("unchecked")
    private void prepareRefresh(AuthClientType oldType, AuthClientType newType, long rotated) {
        var user = user();
        when(jwtService.decodeToken("old-refresh")).thenReturn(payload(oldType));
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("rt:fixture@sqb.test:device")).thenReturn("old-refresh");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(7L)).thenReturn(Optional.of(UserProfile.builder().user(user).profileCompleted(true).build()));
        tokens(newType);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("old-refresh"), eq("new-refresh"))).thenReturn(rotated);
    }

    private void tokens(AuthClientType type) {
        when(jwtService.generateAccessToken(anyMap(), eq("fixture@sqb.test"))).thenReturn("access");
        when(jwtService.generateRefreshToken(eq("fixture@sqb.test"), eq(7L), eq("device"), anyString(), isNull(), eq(type)))
                .thenReturn("new-refresh");
    }

    private void authenticate(JwtPayload payload) {
        var auth = new UsernamePasswordAuthenticationToken("fixture", null, List.of());
        auth.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void assertCode(ResponseCode expected, org.junit.jupiter.api.function.Executable call) {
        assertEquals(expected, assertThrows(AppException.class, call).getResponseCode());
    }
}
