package com.frozenheart.backend.modules.notification.listener;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent;
import com.frozenheart.backend.modules.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationEventListener {

    private final PushNotificationService pushNotificationService;

    @Async
    @EventListener
    public void handlePushNotificationEvent(PushNotificationEvent event) {
        if (event == null) {
            return;
        }

        try {
            if (event.getUserIds() != null && !event.getUserIds().isEmpty()) {
                pushNotificationService.sendPushBatch(
                        event.getUserIds(),
                        event.getTitle(),
                        event.getBody(),
                        event.getIconUrl(),
                        event.getTargetType(),
                        event.getTargetId(),
                        event.getTargetUrl(),
                        event.getPushType()
                );
            }
        } catch (Exception e) {
            log.error("[PushNotificationEventListener] Lỗi khi xử lý sự kiện PushNotification: {}", e.getMessage());
        }
    }
}
