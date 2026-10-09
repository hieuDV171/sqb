package com.frozenheart.backend.modules.notification.service.impl;

import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.notification.service.PushNotificationService;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserPushSettingRepository userPushSettingRepository;
    private final UserProfileRepository userProfileRepository;

    private final Optional<FirebaseMessaging> firebaseMessaging;

    @Override
    @Transactional
    public void sendPush(Long userId, String title, String body, String iconUrl,
                         NotificationTargetType targetType, Long targetId, String targetUrl,
                         PushNotificationType pushType) {
        if (userId == null || firebaseMessaging.isEmpty()) {
            return;
        }

        if (!isPushAllowedForUser(userId, pushType)) {
            log.debug("[PushNotificationService] Push notification bị chặn bởi cài đặt hoặc khung giờ yên lặng của user {}", userId);
            return;
        }

        List<UserDevice> activeDevices = userDeviceRepository.findByUserIdAndIsActiveTrue(userId);
        if (activeDevices.isEmpty()) {
            return;
        }

        List<String> fids = activeDevices.stream()
                .map(UserDevice::getFid)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        if (fids.isEmpty()) {
            return;
        }

        sendMulticastToDevices(fids, activeDevices, title, body, iconUrl, targetType, targetId, targetUrl, pushType);
    }

    @Override
    @Transactional
    public void sendPushBatch(List<Long> userIds, String title, String body, String iconUrl,
                              NotificationTargetType targetType, Long targetId, String targetUrl,
                              PushNotificationType pushType) {
        if (userIds == null || userIds.isEmpty() || firebaseMessaging.isEmpty()) {
            return;
        }

        List<Long> distinctUserIds = userIds.stream().filter(Objects::nonNull).distinct().toList();

        // 1. Lọc các user cho phép nhận push
        Map<Long, UserPushSetting> settingsMap = userPushSettingRepository.findAllById(distinctUserIds).stream()
                .collect(Collectors.toMap(UserPushSetting::getUserId, s -> s));

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(distinctUserIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<Long> allowedUserIds = distinctUserIds.stream()
                .filter(uid -> {
                    UserProfile profile = profileMap.get(uid);
                    String userTz = (profile != null && profile.getTimezone() != null && !profile.getTimezone().isBlank())
                            ? profile.getTimezone() : Time.DEFAULT_TIMEZONE;
                    return isPushAllowed(settingsMap.get(uid), pushType, userTz);
                })
                .toList();

        if (allowedUserIds.isEmpty()) {
            return;
        }

        // 2. Lấy thiết bị active theo lô (0 N+1)
        List<UserDevice> activeDevices = userDeviceRepository.findActiveDevicesByUserIds(allowedUserIds);
        if (activeDevices.isEmpty()) {
            return;
        }

        List<String> fids = activeDevices.stream()
                .map(UserDevice::getFid)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        if (fids.isEmpty()) {
            return;
        }

        // Chia nhóm tối đa 500 FID/lần gửi theo giới hạn của FCM Multicast
        int batchSize = 500;
        for (int i = 0; i < fids.size(); i += batchSize) {
            List<String> subFids = fids.subList(i, Math.min(i + batchSize, fids.size()));
            sendMulticastToDevices(subFids, activeDevices, title, body, iconUrl, targetType, targetId, targetUrl, pushType);
        }
    }

    private void sendMulticastToDevices(List<String> fids, List<UserDevice> devices,
                                        String title, String body, String iconUrl,
                                        NotificationTargetType targetType, Long targetId,
                                        String targetUrl, PushNotificationType pushType) {

        if (firebaseMessaging.isEmpty()) {
            return;
        }

        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllFids(fids)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setImage(iconUrl)
                            .build());

            if (targetType != null) messageBuilder.putData("targetType", targetType.name());
            if (targetId != null) messageBuilder.putData("targetId", String.valueOf(targetId));
            if (targetUrl != null) messageBuilder.putData("targetUrl", targetUrl);
            if (pushType != null) messageBuilder.putData("pushType", pushType.name());

            MulticastMessage message = messageBuilder.build();
            BatchResponse response = firebaseMessaging.get().sendEachForMulticast(message);

            log.info("[PushNotificationService] Đã gửi thông báo đẩy: {} thành công, {} thất bại trong tổng số {} thiết bị",
                    response.getSuccessCount(), response.getFailureCount(), fids.size());

            // Tự động dọn dẹp các FID đã hết hạn / không hợp lệ
            if (response.getFailureCount() > 0) {
                handleFailedFids(response.getResponses(), fids, devices);
            }
        } catch (Exception e) {
            log.error("[PushNotificationService] Lỗi khi gửi multicast push notification: {}", e.getMessage());
        }
    }

    private void handleFailedFids(List<SendResponse> responses, List<String> fids, List<UserDevice> devices) {
        Map<String, UserDevice> deviceMap = devices.stream()
                .filter(d -> d.getFid() != null)
                .collect(Collectors.toMap(UserDevice::getFid, d -> d, (d1, _) -> d1));

        List<UserDevice> invalidDevices = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse res = responses.get(i);
            if (!res.isSuccessful()) {
                MessagingErrorCode errorCode = res.getException() != null ? res.getException().getMessagingErrorCode() : null;
                if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    String failedFid = fids.get(i);
                    UserDevice failedDevice = deviceMap.get(failedFid);
                    if (failedDevice != null) {
                        failedDevice.setActive(false);
                        invalidDevices.add(failedDevice);
                    }
                }
            }
        }

        if (!invalidDevices.isEmpty()) {
            userDeviceRepository.saveAll(invalidDevices);
            log.info("[PushNotificationService] Đã vô hiệu hóa {} thiết bị có FID không hợp lệ", invalidDevices.size());
        }
    }

    private boolean isPushAllowedForUser(Long userId, PushNotificationType pushType) {
        UserPushSetting setting = userPushSettingRepository.findById(userId).orElse(null);
        String userTz = userProfileRepository.findByUserId(userId)
                .map(UserProfile::getTimezone)
                .filter(tz -> !tz.isBlank())
                .orElse(Time.DEFAULT_TIMEZONE);
        return isPushAllowed(setting, pushType, userTz);
    }

    private boolean isPushAllowed(UserPushSetting setting, PushNotificationType pushType, String userTimezone) {
        PushPreferences prefs = (setting != null && setting.getPreferences() != null)
                ? setting.getPreferences()
                : PushPreferences.createDefault();

        if (!prefs.isPushEnabled()) {
            return false;
        }

        // Kiểm tra khung giờ yên lặng (Quiet Hours) theo timezone của user
        if (prefs.getQuietHour() != null && prefs.getQuietHour().isEnabled()) {
            ZoneId userZone;
            try {
                userZone = ZoneId.of(userTimezone != null && !userTimezone.isBlank() ? userTimezone : Time.DEFAULT_TIMEZONE);
            } catch (Exception e) {
                userZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
            }
            LocalTime now = LocalTime.now(userZone);
            LocalTime start = prefs.getQuietHour().getStartTime() != null ? prefs.getQuietHour().getStartTime() : LocalTime.of(22, 0);
            LocalTime end = prefs.getQuietHour().getEndTime() != null ? prefs.getQuietHour().getEndTime() : LocalTime.of(7, 0);

            if (isInQuietHour(now, start, end)) {
                return false;
            }
        }

        // Kiểm tra cài đặt danh mục
        if (pushType != null && prefs.getCategories() != null) {
            CategorySetting catSetting = prefs.getCategories().get(pushType);
            return catSetting == null || catSetting.isEnabled();
        }

        return true;
    }

    // start = end -> luôn đúng -> khoảng thời gian yên lặng = 24 giờ = tắt thông báo
    private boolean isInQuietHour(LocalTime now, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            // Trường hợp qua đêm, ví dụ 22:00 -> 07:00 hôm sau
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}
