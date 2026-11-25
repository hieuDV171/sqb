package com.frozenheart.backend.modules.notification.service.impl;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent.Delivery;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final int FCM_MULTICAST_LIMIT = 500;
    private static final String PUSH_NOTIFICATION_ID_PARAM = "pushNotificationId";

    private final UserDeviceRepository userDeviceRepository;
    private final UserPushSettingRepository userPushSettingRepository;
    private final UserProfileRepository userProfileRepository;

    private final Optional<FirebaseMessaging> firebaseMessaging;

    @Override
    @Transactional
    public void sendPushBatch(List<Delivery> deliveries, PushNotificationType pushType) {
        if (deliveries.isEmpty() || firebaseMessaging.isEmpty()) {
            return;
        }

        List<Long> distinctUserIds = deliveries.stream().map(Delivery::userId).distinct().toList();

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
        Map<Long, List<UserDevice>> devicesByUser = userDeviceRepository.findActiveDevicesByUserIds(allowedUserIds).stream()
                .filter(device -> device.getFid() != null && !device.getFid().isBlank())
                .collect(Collectors.groupingBy(device -> device.getUser().getId()));

        List<UserDevice> invalidDevices = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            List<UserDevice> devices = devicesByUser.getOrDefault(delivery.userId(), List.of());
            Map<String, UserDevice> devicesByFid = devices.stream()
                    .filter(UserDevice::isActive)
                    .collect(Collectors.toMap(UserDevice::getFid, device -> device, (first, _) -> first));
            List<String> fids = new ArrayList<>(devicesByFid.keySet());
            for (int i = 0; i < fids.size(); i += FCM_MULTICAST_LIMIT) {
                List<String> subFids = fids.subList(i, Math.min(i + FCM_MULTICAST_LIMIT, fids.size()));
                sendMulticastToDevices(subFids, devicesByFid, delivery, pushType, invalidDevices);
            }
        }
        if (!invalidDevices.isEmpty()) {
            userDeviceRepository.saveAll(invalidDevices);
            log.info("[PushNotificationService] Đã vô hiệu hóa {} thiết bị có FID không hợp lệ", invalidDevices.size());
        }
    }

    private void sendMulticastToDevices(List<String> fids, Map<String, UserDevice> devicesByFid,
                                        Delivery delivery, PushNotificationType pushType,
                                        List<UserDevice> invalidDevices) {
        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllFids(fids)
                    .setNotification(Notification.builder()
                            .setTitle(delivery.title())
                            .setBody(delivery.body())
                            .setImage(delivery.iconUrl())
                            .build())
                    .putData("notificationId", delivery.notificationId().toString())
                    .putData("targetUrl", notificationClickUrl(delivery));

            if (delivery.targetType() != null) messageBuilder.putData("targetType", delivery.targetType().name());
            if (delivery.targetId() != null) messageBuilder.putData("targetId", delivery.targetId().toString());
            if (pushType != null) messageBuilder.putData("pushType", pushType.name());

            MulticastMessage message = messageBuilder.build();
            BatchResponse response = firebaseMessaging.get().sendEachForMulticast(message);

            log.info("[PushNotificationService] Đã gửi thông báo đẩy: {} thành công, {} thất bại trong tổng số {} thiết bị",
                    response.getSuccessCount(), response.getFailureCount(), fids.size());

            // Tự động dọn dẹp các FID đã hết hạn / không hợp lệ
            if (response.getFailureCount() > 0) {
                collectInvalidDevices(response.getResponses(), fids, devicesByFid, invalidDevices);
            }
        } catch (Exception e) {
            log.error("[PushNotificationService] Lỗi khi gửi multicast push notification: {}", e.getMessage());
        }
    }

    private static String notificationClickUrl(Delivery delivery) {
        String target = Objects.requireNonNullElse(delivery.targetUrl(), "/notifications");
        // Older workers navigate to targetUrl without reading the separate notificationId field.
        return UriComponentsBuilder.fromUriString(target)
                .replaceQueryParam(PUSH_NOTIFICATION_ID_PARAM, delivery.notificationId())
                .build().toUriString();
    }

    private void collectInvalidDevices(List<SendResponse> responses, List<String> fids,
                                       Map<String, UserDevice> devicesByFid, List<UserDevice> invalidDevices) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse res = responses.get(i);
            if (!res.isSuccessful()) {
                MessagingErrorCode errorCode = res.getException() != null ? res.getException().getMessagingErrorCode() : null;
                if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    UserDevice failedDevice = devicesByFid.get(fids.get(i));
                    failedDevice.setActive(false);
                    invalidDevices.add(failedDevice);
                }
            }
        }
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
