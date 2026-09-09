package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;

import java.util.List;

public interface PushNotificationService {

    void sendPush(Long userId, String title, String body, String iconUrl,
                  NotificationTargetType targetType, Long targetId, String targetUrl,
                  PushNotificationType pushType);

    void sendPushBatch(List<Long> userIds, String title, String body, String iconUrl,
                       NotificationTargetType targetType, Long targetId, String targetUrl,
                       PushNotificationType pushType);

}
