package com.frozenheart.backend.modules.device.dto;

import java.time.Instant;

import com.frozenheart.backend.core.entity.user.DevicePlatform;

public record DeviceResponse(
        Long userDeviceId,
        String deviceId,
        String deviceName,
        DevicePlatform platform,
        String osVersion,
        String appVersion,
        Instant lastActiveAt,
        boolean isActive,
        boolean isCurrentDevice) {

}
