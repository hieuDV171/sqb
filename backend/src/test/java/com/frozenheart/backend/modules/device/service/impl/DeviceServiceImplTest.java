package com.frozenheart.backend.modules.device.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock UserDeviceRepository repository;
    @Mock RedisTemplate<String, String> redisTemplate;
    @InjectMocks DeviceServiceImpl service;

    @BeforeEach
    void authenticate() {
        var authentication = new UsernamePasswordAuthenticationToken("user", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(1L).deviceId("my-device").build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unregisterOnlyUsesAtomicOwnerAndFidMatchAndDoesNotRevokeSession() {
        service.unregisterFid("stale-or-foreign-fid");

        verify(repository).clearMatchingFid(1L, "my-device", "stale-or-foreign-fid");
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void unregisterIgnoresBlankOrNullFid() {
        service.unregisterFid(null);
        service.unregisterFid(" ");

        verifyNoInteractions(repository, redisTemplate);
    }

    @Test
    void syncRefusesFidBoundToAnotherUserOrDeviceWithoutWriting() {
        when(repository.isFidBoundToAnotherDevice("foreign-fid", 1L, "my-device")).thenReturn(true);

        AppException error = assertThrows(AppException.class, () -> service.syncFid("foreign-fid"));

        assertEquals(ResponseCode.FID_ALREADY_BOUND, error.getResponseCode());
        verify(repository).lockFidForTransaction("foreign-fid");
        verify(repository).isFidBoundToAnotherDevice("foreign-fid", 1L, "my-device");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void syncCannotCreateOrReactivateRevokedDevice() {
        AppException error = assertThrows(AppException.class, () -> service.syncFid("fid"));

        assertEquals(ResponseCode.DEVICE_NOT_FOUND, error.getResponseCode());
        verify(repository).syncActiveDeviceFid(eq(1L), eq("my-device"), eq("fid"), any());
        verifyNoInteractions(redisTemplate);
    }
}
