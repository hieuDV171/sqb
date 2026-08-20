package com.frozenheart.backend.modules.device.service;

import com.frozenheart.backend.modules.device.dto.DeviceListResponse;

public interface DeviceService {
    void revokeDevice(String targetDeviceId);

    DeviceListResponse getMyDevices();

    void deleteDevice(String targetDeviceId);
}
