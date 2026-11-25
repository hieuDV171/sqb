package com.frozenheart.backend.core.config;

import com.frozenheart.backend.core.dto.event.AccountDeletedEvent;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.security.AccountState;
import com.frozenheart.backend.core.security.AccountWebSocketSessions;
import com.frozenheart.backend.core.security.JwtService;
import com.frozenheart.backend.core.security.TokenType;
import com.frozenheart.backend.core.security.WebSocketAuthInterceptor;
import com.frozenheart.backend.modules.conversation.controller.ChatWebSocketController;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.conversation.service.ChatRealtimePublisher;
import com.frozenheart.backend.modules.conversation.service.HiddenChatAccess;
import com.frozenheart.backend.modules.conversation.service.HiddenChatOutboundGuard;
import com.frozenheart.backend.modules.conversation.service.impl.ChatWebSocketServiceImpl;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatTypingStompTest {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

    @Test
    void typingFramesReachTheOtherUsersPrivateQueue() throws Exception {
        var participants = mock(UserParticipantRepository.class);
        when(participants.findByConversationIdAndLeftAtIsNull(42L)).thenReturn(List.of(
                UserParticipant.builder().user(User.builder().id(1L).email("sender@sqb.edu.vn").build()).build(),
                UserParticipant.builder().user(User.builder().id(2L).email("receiver@sqb.edu.vn").build()).build()));
        try (var context = startBroker(participants, Map.of("sender", 1L, "receiver", 2L))) {
            var sessions = context.getBean(AccountWebSocketSessions.class);
            var handler = sessions.decorate(context.getBean("subProtocolWebSocketHandler", WebSocketHandler.class));
            var senderFrames = new ArrayList<String>();
            var receiverFrames = new ArrayList<String>();
            var sender = connect(handler, "sender", senderFrames);
            var receiver = connect(handler, "receiver", receiverFrames);

            for (String payload : List.of(
                    "{\"conversation_id\":42,\"typing\":true}",
                    "{\"conversation_id\":42,\"typing\":null}",
                    "{\"conversation_id\":42}",
                    "{\"conversation_id\":42,\"typing\":false}",
                    "{\"conversation_id\":42,\"is_typing\":true}")) {
                publishTyping(handler, sender, payload);
            }

            assertTrue(receivedEvents(senderFrames).isEmpty());
            var events = receivedEvents(receiverFrames);
            assertEquals(2, events.size(), receiverFrames::toString);
            assertEquals(List.of(true, false), events.stream().map(event -> event.get("typing").asBoolean()).toList());
            for (var event : events) {
                assertEquals("TYPING_STATUS", event.get("event_type").asString());
                assertEquals(42L, event.get("conversation_id").asLong());
                assertEquals(1L, event.get("user").get("user_id").asLong());
            }

            sessions.onAccountsDeleted(new AccountDeletedEvent(List.of(1L)));
            when(sender.isOpen()).thenReturn(false);
            handler.afterConnectionClosed(sender, CloseStatus.POLICY_VIOLATION);
            sessions.onAccountsDeleted(new AccountDeletedEvent(List.of(1L)));
            verify(sender).close(CloseStatus.POLICY_VIOLATION);
            verify(receiver, never()).close(any());
        }
    }

    @Test
    void groupTypingReachesEveryOtherActiveMemberAndKeepsEachTypistSeparate() throws Exception {
        var group = Conversation.builder().id(84L).type(ConversationType.GROUP).build();
        var first = groupMember(group, 1L, "first");
        var second = groupMember(group, 2L, "second");
        var observer = groupMember(group, 3L, "observer");
        var former = groupMember(group, 4L, "former");
        former.setLeftAt(Instant.now());
        var participants = mock(UserParticipantRepository.class);
        when(participants.findByConversationIdAndLeftAtIsNull(group.getId()))
                .thenReturn(List.of(first, second, observer, former).stream()
                        .filter(member -> member.getLeftAt() == null).toList());

        try (var context = startBroker(participants, Map.of("first", 1L, "second", 2L, "observer", 3L, "former", 4L))) {
            var sessions = context.getBean(AccountWebSocketSessions.class);
            var handler = sessions.decorate(context.getBean("subProtocolWebSocketHandler", WebSocketHandler.class));
            var firstFrames = new ArrayList<String>();
            var secondFrames = new ArrayList<String>();
            var observerFrames = new ArrayList<String>();
            var formerFrames = new ArrayList<String>();
            var firstSocket = connect(handler, "first", firstFrames);
            var secondSocket = connect(handler, "second", secondFrames);
            connect(handler, "observer", observerFrames);
            var formerSocket = connect(handler, "former", formerFrames);

            publishTyping(handler, firstSocket, "{\"conversation_id\":84,\"typing\":true}");
            publishTyping(handler, secondSocket, "{\"conversation_id\":84,\"typing\":true}");
            publishTyping(handler, formerSocket, "{\"conversation_id\":84,\"typing\":true}");
            publishTyping(handler, firstSocket, "{\"conversation_id\":84,\"typing\":false}");
            publishTyping(handler, secondSocket, "{\"conversation_id\":84,\"typing\":false}");

            assertEquals(List.of(2L, 2L), typingUserIds(receivedEvents(firstFrames)));
            assertEquals(List.of(1L, 1L), typingUserIds(receivedEvents(secondFrames)));
            assertTrue(receivedEvents(formerFrames).isEmpty());
            var events = receivedEvents(observerFrames);
            assertEquals(List.of(1L, 2L, 1L, 2L), typingUserIds(events));
            assertEquals(List.of(true, true, false, false),
                    events.stream().map(event -> event.get("typing").asBoolean()).toList());
            for (var event : events) {
                assertEquals("TYPING_STATUS", event.get("event_type").asString());
                assertEquals(group.getId().longValue(), event.get("conversation_id").asLong());
            }
        }
    }

    private static AnnotationConfigWebApplicationContext startBroker(
            UserParticipantRepository participants, Map<String, Long> accounts) {
        var jwt = mock(JwtService.class);
        var userDetails = mock(UserDetailsService.class);
        accounts.forEach((token, userId) -> allowLogin(jwt, userDetails, token, userId));
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("jsonMapper", MAPPER);
            factory.registerSingleton("jwtService", jwt);
            factory.registerSingleton("userDetailsService", userDetails);
            factory.registerSingleton("authInterceptor", new WebSocketAuthInterceptor(jwt, userDetails));
            factory.registerSingleton("hiddenChatAccess", mock(HiddenChatAccess.class));
            factory.registerSingleton("participants", participants);
            factory.registerSingleton("userProfiles", mock(UserProfileRepository.class));
        });
        context.register(WebSocketConfig.class, AccountWebSocketSessions.class, SynchronousChannels.class,
                HiddenChatOutboundGuard.class, ChatRealtimePublisher.class, TypingServiceConfig.class,
                ChatWebSocketController.class);
        context.refresh();
        return context;
    }

    private static UserParticipant groupMember(Conversation group, long userId, String username) {
        return UserParticipant.builder().conversation(group)
                .user(User.builder().id(userId).email(username + "@sqb.edu.vn").build()).build();
    }

    private static void publishTyping(WebSocketHandler handler, WebSocketSession session, String payload) throws Exception {
        handler.handleMessage(session, frame("SEND\ndestination:/app/chat.typing\n\n" + payload));
    }

    private static List<JsonNode> receivedEvents(List<String> frames) {
        return frames.stream().filter(frame -> frame.startsWith("MESSAGE"))
                .map(frame -> MAPPER.readTree(frame.substring(frame.indexOf("\n\n") + 2, frame.lastIndexOf('\0'))))
                .toList();
    }

    private static List<Long> typingUserIds(List<JsonNode> events) {
        return events.stream().map(event -> event.get("user").get("user_id").asLong()).toList();
    }

    private static void allowLogin(JwtService jwt, UserDetailsService service, String token, long id) {
        var username = token + "@sqb.edu.vn";
        var user = org.springframework.security.core.userdetails.User.withUsername(username)
                .password("unused").roles("STUDENT").build();
        var payload = JwtPayload.builder().userId(id).username(username)
                .tokenType(TokenType.ACCESS).accountState(AccountState.ACTIVE).build();
        when(jwt.decodeToken(token)).thenReturn(payload);
        when(service.loadUserByUsername(username)).thenReturn(user);
        when(jwt.isTokenValid(payload, user)).thenReturn(true);
    }

    private static WebSocketSession connect(WebSocketHandler handler, String token, List<String> frames) throws Exception {
        var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn(token);
        when(session.getAcceptedProtocol()).thenReturn("v12.stomp");
        when(session.getAttributes()).thenReturn(new HashMap<>());
        doAnswer(call -> {
            frames.add(call.<TextMessage>getArgument(0).getPayload());
            return null;
        }).when(session).sendMessage(any());
        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, frame("CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer " + token + "\n\n"));
        assertTrue(frames.stream().anyMatch(frame -> frame.startsWith("CONNECTED")), frames::toString);
        handler.handleMessage(session, frame("SUBSCRIBE\nid:chat\ndestination:/user/queue/messages\n\n"));
        return session;
    }

    private static TextMessage frame(String data) {
        return new TextMessage(data + '\0');
    }

    @Configuration(proxyBeanMethods = false)
    static class SynchronousChannels implements WebSocketMessageBrokerConfigurer {
        @Override
        public void configureClientInboundChannel(ChannelRegistration registration) {
            registration.executor(new SyncTaskExecutor());
        }

        @Override
        public void configureClientOutboundChannel(ChannelRegistration registration) {
            registration.executor(new SyncTaskExecutor());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TypingServiceConfig {
        @Bean
        ChatWebSocketServiceImpl chatWebSocketService(ChatRealtimePublisher publisher, UserParticipantRepository participants,
                UserProfileRepository profiles, HiddenChatAccess hidden) {
            return new ChatWebSocketServiceImpl(publisher, null, null, null, participants, null, profiles, null, null, hidden);
        }
    }
}
