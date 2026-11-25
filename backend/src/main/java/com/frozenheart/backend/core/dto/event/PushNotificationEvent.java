package com.frozenheart.backend.core.dto.event;

import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;

import java.util.List;

public record PushNotificationEvent(List<Delivery> deliveries, PushNotificationType pushType) {
    public record Delivery(Long notificationId, Long userId, String title, String body, String iconUrl,
                           NotificationTargetType targetType, Long targetId, String targetUrl) {}
}
