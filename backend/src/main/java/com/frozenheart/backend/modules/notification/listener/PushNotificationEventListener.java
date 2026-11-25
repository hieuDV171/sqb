package com.frozenheart.backend.modules.notification.listener;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent;
import com.frozenheart.backend.modules.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationEventListener {

    private final PushNotificationService pushNotificationService;

    @Async
    @TransactionalEventListener
    public void handlePushNotificationEvent(PushNotificationEvent event) {
        try {
            pushNotificationService.sendPushBatch(event.deliveries(), event.pushType());
        } catch (Exception e) {
            log.error("[PushNotificationEventListener] Lỗi khi xử lý sự kiện PushNotification: {}", e.getMessage());
        }
    }
}
