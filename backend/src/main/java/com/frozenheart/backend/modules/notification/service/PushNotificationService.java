package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent.Delivery;
import com.frozenheart.backend.core.entity.user.PushNotificationType;

import java.util.List;

public interface PushNotificationService {

    void sendPushBatch(List<Delivery> deliveries, PushNotificationType pushType);

}
