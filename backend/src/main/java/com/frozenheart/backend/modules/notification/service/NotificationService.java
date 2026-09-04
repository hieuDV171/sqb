package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.modules.notification.dto.*;

public interface NotificationService {

    NotificationListResponseDto getMyNotifications(Long after, Integer limit);

    ReadNotificationResponseDto markNotificationAsRead(Long notificationId);

    void markAllNotificationsAsRead();

    PushSettingsResponseDto getPushSettings();

    UpdatePushSettingsResponseDto updatePushSettings(UpdatePushSettingsRequestDto request);
}
