package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationWriter {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void create(Notification notification) {
        notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationsCreated(List.of(notification.getReceiver().getId())));
    }

    @Transactional
    public void createAll(List<Notification> notifications) {
        if (notifications.isEmpty()) return;

        notificationRepository.saveAll(notifications);
        List<Long> receiverIds = notifications.stream()
                .map(notification -> notification.getReceiver().getId())
                .distinct()
                .toList();
        eventPublisher.publishEvent(new NotificationsCreated(receiverIds));
    }

    @TransactionalEventListener
    public void notifyRecipients(NotificationsCreated event) {
        for (Long receiverId : event.receiverIds()) {
            messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/notifications",
                    Map.of("event_type", "NOTIFICATIONS_CHANGED"));
        }
    }

    public record NotificationsCreated(List<Long> receiverIds) {}
}
