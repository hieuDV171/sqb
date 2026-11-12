package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupMemberRemovedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupMembersAddedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsGroupRolesChangedDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsMessageRevokedDto;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatRealtimePublisherTest {

    @ParameterizedTest
    @MethodSource("conversationEvents")
    void publishesAfterCommitToActiveMembersAndMarksHiddenRecipients(Object event) {
        var messaging = mock(SimpMessagingTemplate.class);
        var participants = mock(UserParticipantRepository.class);
        var normal = UserParticipant.builder().user(User.builder().id(1L).build()).build();
        var hidden = UserParticipant.builder().user(User.builder().id(2L).build()).hiddenAt(Instant.now()).build();
        when(participants.findByConversationIdAndLeftAtIsNull(10L)).thenReturn(List.of(normal, hidden));
        if (event instanceof WsGroupMemberRemovedDto) {
            var removed = UserParticipant.builder().leftAt(Instant.now()).hiddenAt(Instant.now())
                    .removedFromGroup(true).build();
            when(participants.findByUserIdAndConversationId(3L, 10L)).thenReturn(Optional.of(removed));
        }
        var transaction = new TransactionTemplate(new InMemoryTransactionManager());

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(TransactionalEventListenerFactory.class);
            context.registerBean(ChatRealtimePublisher.class, () -> new ChatRealtimePublisher(messaging, participants));
            context.refresh();

            transaction.executeWithoutResult(status -> {
                context.publishEvent(event);
                status.setRollbackOnly();
            });
            verifyNoInteractions(messaging);

            transaction.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(messaging);
            });
        }

        verify(messaging).convertAndSendToUser("1", "/queue/messages", event);
        verify(messaging).convertAndSendToUser(eq("2"), eq("/queue/messages"), eq(event),
                argThat((Map<String, Object> headers) -> {
                    assertEquals(Map.of("sqb-hidden-chat", List.of("true")),
                            headers.get(NativeMessageHeaderAccessor.NATIVE_HEADERS));
                    return true;
                }));
        if (event instanceof WsGroupMemberRemovedDto) {
            verify(messaging).convertAndSendToUser(eq("3"), eq("/queue/messages"), eq(event),
                    argThat((Map<String, Object> headers) ->
                            Map.of("sqb-hidden-chat", List.of("true"))
                                    .equals(headers.get(NativeMessageHeaderAccessor.NATIVE_HEADERS))));
        }
    }

    private static List<Object> conversationEvents() {
        return List.of(new WsMessageRevokedDto(10L, 25L), new WsGroupRolesChangedDto(10L),
                new WsGroupMemberRemovedDto(10L, 3L), new WsGroupMembersAddedDto(10L));
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
