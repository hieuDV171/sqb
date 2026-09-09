package com.frozenheart.backend.modules.device.service.impl;

import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.UserDevice;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.device.dto.DeviceListResponse;
import com.frozenheart.backend.modules.device.dto.DeviceResponse;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.device.service.DeviceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public DeviceListResponse getMyDevices() {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String currentDeviceId = payload.getDeviceId();

        List<UserDevice> devices = userDeviceRepository.findByUserIdOrderByLastActiveAtDesc(currentUserId);

        List<DeviceResponse> deviceResponses = devices.stream()
                .map(d -> {
                    String deviceId = d.getDeviceId();
                    return new DeviceResponse(
                            d.getId(),
                            deviceId,
                            d.getDeviceName(),
                            d.getPlatform(),
                            d.getOsVersion(),
                            d.getAppVersion(),
                            d.getLastActiveAt(),
                            d.isActive(),
                            deviceId != null && deviceId.equals(currentDeviceId));
                })
                .toList();

        return new DeviceListResponse(deviceResponses);
    }

    @Override
    @Transactional
    public void revokeDevice(String targetDeviceId) {

        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String currentEmail = payload.getUsername();

        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(currentUserId, targetDeviceId)
                .orElseThrow(() -> new AppException(ResponseCode.DEVICE_NOT_FOUND));

        device.setActive(false);
        device.setFcmToken(null);
        userDeviceRepository.save(device);

        String redisKey = "rt:" + currentEmail + ":" + targetDeviceId;
        redisTemplate.delete(redisKey);
    }

    @Override
    @Transactional
    public void deleteDevice(String targetDeviceId) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String currentEmail = payload.getUsername();

        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(currentUserId, targetDeviceId)
                .orElseThrow(() -> new AppException(ResponseCode.DEVICE_NOT_FOUND));

        if (device.isActive()) {
            String redisKey = "rt:" + currentEmail + ":" + targetDeviceId;
            redisTemplate.delete(redisKey);
        }

        userDeviceRepository.delete(device);
    }

}
