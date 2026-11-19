package com.frozenheart.backend.modules.auth.controller;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.user.DevicePlatform;
import com.frozenheart.backend.core.security.AuthCompatibilityPolicy;
import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;
import com.frozenheart.backend.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCompatibilityResponseTest {
    @Mock AuthService authService;
    @InjectMocks AuthController controller;

    @Test void legacyLoginWarnsWithoutChangingSuccessCodeOrData() {
        var request = login(DevicePlatform.ANDROID);
        var data = AuthResponse.builder().accessToken("fixture-at").refreshToken("fixture-rt").build();
        when(authService.login(request)).thenReturn(data);
        var response = controller.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ResponseCode.SUCCESS.getCode(), response.getBody().getCode());
        assertEquals(AuthCompatibilityPolicy.WARNING_MESSAGE, response.getBody().getMessage());
        assertSame(data, response.getBody().getData());
    }

    @Test void webLoginKeepsNormalSuccessMessage() {
        var request = login(DevicePlatform.WEB);
        when(authService.login(request)).thenReturn(AuthResponse.builder().accessToken("fixture-at").build());
        var response = controller.login(request).getBody();
        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.getMessage(), response.getMessage());
        assertNull(response.getData().refreshToken());
    }

    @Test void jsonRefreshWarnsWithoutChangingSuccessCodeOrTokenPayload() {
        var request = new RefreshTokenRequest("fixture-old-rt");
        var data = RefreshTokenResponse.builder().accessToken("fixture-at").refreshToken("fixture-new-rt").build();
        when(authService.refreshToken(request)).thenReturn(data);
        var response = controller.refreshToken(request).getBody();
        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        assertEquals(AuthCompatibilityPolicy.WARNING_MESSAGE, response.getMessage());
        assertSame(data, response.getData());
    }

    @Test void cookieRefreshKeepsNormalSuccessMessage() {
        when(authService.refreshToken(null)).thenReturn(RefreshTokenResponse.builder().accessToken("fixture-at").build());
        var response = controller.refreshToken(null).getBody();
        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.getMessage(), response.getMessage());
        assertNull(response.getData().refreshToken());
    }

    private LoginRequest login(DevicePlatform platform) {
        return new LoginRequest("fixture@sqb.test", "password", "device", null, platform, null, null, null);
    }
}
