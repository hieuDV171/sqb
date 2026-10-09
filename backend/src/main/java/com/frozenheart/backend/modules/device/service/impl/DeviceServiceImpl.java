package com.frozenheart.backend.modules.device.service.impl;

import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserDevice;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.device.dto.DeviceListResponse;
import com.frozenheart.backend.modules.device.dto.DeviceResponse;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.device.service.DeviceService;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
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
        device.setFid(null);
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

    @Override
    @Transactional
    public void unregisterFid(String fid) {
        if (fid == null || fid.isBlank()) {
            return;
        }
        userDeviceRepository.findByFid(fid).ifPresent(device -> {
            device.setFid(null);
            device.setActive(false);
            userDeviceRepository.save(device);
            log.info("[DeviceService] Đã hủy đăng ký FID thành công cho thiết bị {}", device.getDeviceId());
        });
    }

    @Override
    @Transactional
    public void syncFid(String fid) {
        if (fid == null || fid.isBlank()) {
            return;
        }

        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String currentDeviceId = payload.getDeviceId();

        // 1. Nếu FID này trước đó đang gắn với một thiết bị khác, gỡ FID khỏi thiết bị cũ
        userDeviceRepository.findByFid(fid).ifPresent(oldDevice -> {
            if (!oldDevice.getDeviceId().equals(currentDeviceId)) {
                oldDevice.setFid(null);
                userDeviceRepository.save(oldDevice);
                log.info("[DeviceService] Đã gỡ FID {} khỏi thiết bị cũ {}", fid, oldDevice.getDeviceId());
            }
        });

        // 2. Cập nhật hoặc tạo mới thiết bị hiện tại
        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(currentUserId, currentDeviceId)
                .orElseGet(() -> {
                    User user = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));
                    return UserDevice.builder()
                            .user(user)
                            .deviceId(currentDeviceId)
                            .createdAt(Instant.now())
                            .build();
                });

        device.setFid(fid);
        device.setActive(true);
        device.setLastActiveAt(Instant.now());
        userDeviceRepository.save(device);

        log.info("[DeviceService] Đã đồng bộ FID mới thành công cho user {} trên thiết bị {}", currentUserId, currentDeviceId);
    }

}
