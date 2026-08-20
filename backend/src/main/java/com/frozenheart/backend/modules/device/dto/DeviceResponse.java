package com.frozenheart.backend.modules.device.dto;

import java.time.LocalDateTime;

import com.frozenheart.backend.core.entity.user.DevicePlatform;

public record DeviceResponse(
        Long id,
        String deviceId,
        String deviceName,
        DevicePlatform platform,
        String osVersion,
        String appVersion,
        LocalDateTime lastActiveAt,
        boolean isActive,
        boolean isCurrentDevice
    ) {

}
