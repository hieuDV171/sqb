package com.frozenheart.backend.modules.notification.service;

import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
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
