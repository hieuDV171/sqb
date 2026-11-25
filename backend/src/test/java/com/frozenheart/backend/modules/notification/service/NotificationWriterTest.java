package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent.Delivery;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.notification.listener.PushNotificationEventListener;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
import com.frozenheart.backend.modules.notification.service.NotificationWriter.PushMessage;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class NotificationWriterTest {

    @Test
    void sendsOnlyAfterCommitAndOnlyOncePerRecipientInABatch() {
        var repository = mock(NotificationRepository.class);
        var messaging = mock(SimpMessagingTemplate.class);
        var transaction = new TransactionTemplate(new InMemoryTransactionManager());
        var rolledBack = notificationFor(1L);
        var batch = List.of(notificationFor(2L), notificationFor(2L), notificationFor(3L));

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(TransactionalEventListenerFactory.class);
            context.registerBean(NotificationWriter.class, () -> new NotificationWriter(repository, context, messaging));
            context.refresh();
            var writer = context.getBean(NotificationWriter.class);

            transaction.executeWithoutResult(status -> {
                writer.create(rolledBack);
                verifyNoInteractions(messaging);
                status.setRollbackOnly();
            });
            verifyNoInteractions(messaging);

            transaction.executeWithoutResult(status -> {
                writer.createAll(batch);
                verifyNoInteractions(messaging);
            });
        }

        var event = Map.of("event_type", "NOTIFICATIONS_CHANGED");
        verify(repository).save(rolledBack);
        verify(repository).saveAll(batch);
        verify(messaging).convertAndSendToUser("2", "/queue/notifications", event);
        verify(messaging).convertAndSendToUser("3", "/queue/notifications", event);
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void sendsEachSavedNotificationIdAfterCommitEvenWhenDestinationsMatch() {
        var repository = mock(NotificationRepository.class);
        var messaging = mock(SimpMessagingTemplate.class);
        var pushService = mock(PushNotificationService.class);
        var transaction = new TransactionTemplate(new InMemoryTransactionManager());
        var first = notificationFor(1L);
        var second = notificationFor(1L);
        var otherRecipient = notificationFor(2L);
        var notifications = List.of(first, second, otherRecipient);
        for (Notification notification : notifications) {
            notification.setTargetType(NotificationTargetType.SESSION);
            notification.setTargetId(500L);
        }
        when(repository.saveAll(notifications)).thenAnswer(invocation -> {
            first.setId(101L);
            second.setId(102L);
            otherRecipient.setId(103L);
            return notifications;
        });
        when(repository.save(first)).thenReturn(first);

        var messages = List.of(new PushMessage(first, "Push 1", null, "/sessions/500"),
                new PushMessage(second, "Push 2", null, "/sessions/500"),
                new PushMessage(otherRecipient, "Push 3", null, "/sessions/500"));
        var pushType = PushNotificationType.ACADEMIC;
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(TransactionalEventListenerFactory.class);
            context.registerBean(NotificationWriter.class, () -> new NotificationWriter(repository, context, messaging));
            context.registerBean(PushNotificationEventListener.class, () -> new PushNotificationEventListener(pushService));
            context.refresh();
            var writer = context.getBean(NotificationWriter.class);

            transaction.executeWithoutResult(status -> {
                writer.createWithPush(messages.getFirst(), pushType);
                status.setRollbackOnly();
            });
            verifyNoInteractions(pushService);

            transaction.executeWithoutResult(status -> {
                writer.createAllWithPush(messages, pushType);
                verifyNoInteractions(pushService);
            });
        }

        verify(pushService).sendPushBatch(List.of(
                new Delivery(101L, 1L, "Push 1", null, null, NotificationTargetType.SESSION, 500L, "/sessions/500"),
                new Delivery(102L, 1L, "Push 2", null, null, NotificationTargetType.SESSION, 500L, "/sessions/500"),
                new Delivery(103L, 2L, "Push 3", null, null, NotificationTargetType.SESSION, 500L, "/sessions/500")), pushType);
        verifyNoMoreInteractions(pushService);
    }

    private static Notification notificationFor(long userId) {
        return Notification.builder().receiver(User.builder().id(userId).build()).build();
    }

    private static class InMemoryTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
