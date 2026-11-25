package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
        publishCreated(notifications);
    }

    @Transactional
    public void createWithPush(PushMessage message, PushNotificationType pushType) {
        Notification saved = notificationRepository.save(message.notification());
        publishCreated(List.of(saved));
        eventPublisher.publishEvent(new PushNotificationEvent(List.of(message.deliveryFor(saved)), pushType));
    }

    @Transactional
    public void createAllWithPush(List<PushMessage> messages, PushNotificationType pushType) {
        if (messages.isEmpty()) return;

        List<Notification> saved = notificationRepository.saveAll(messages.stream().map(PushMessage::notification).toList());
        publishCreated(saved);
        List<PushNotificationEvent.Delivery> deliveries = IntStream.range(0, saved.size())
                .mapToObj(index -> messages.get(index).deliveryFor(saved.get(index)))
                .toList();
        eventPublisher.publishEvent(new PushNotificationEvent(deliveries, pushType));
    }

    private void publishCreated(List<Notification> notifications) {
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

    public record PushMessage(Notification notification, String title, String body, String targetUrl) {
        public PushMessage(Notification notification) {
            this(notification, notification.getTitle(), notification.getBody(), notification.getTargetUrl());
        }

        private PushNotificationEvent.Delivery deliveryFor(Notification saved) {
            return new PushNotificationEvent.Delivery(saved.getId(), saved.getReceiver().getId(), title, body,
                    saved.getIconUrl(), saved.getTargetType(), saved.getTargetId(), targetUrl);
        }
    }
}
