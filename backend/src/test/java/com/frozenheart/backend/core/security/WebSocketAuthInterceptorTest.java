package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.security.Principal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;

    @Test
    void rejectsOnboardingTokenBeforeStompAuthentication() {
        WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer onboarding-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.decodeToken("onboarding-token")).thenReturn(JwtPayload.builder()
                .username("student@sqb.edu.vn")
                .tokenType(TokenType.ONBOARDING)
                .accountState(AccountState.EMAIL_VERIFICATION_REQUIRED)
                .build());

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void resolvesChatQueueByAccountIdForEachConnectedUser() {
        var registry = new DefaultSimpUserRegistry();
        var resolver = new DefaultUserDestinationResolver(registry);
        Principal sender = authenticate("sender", 10L);
        Principal receiver = authenticate("receiver", 20L);

        registry.onApplicationEvent(new SessionConnectedEvent(this,
                queueMessage(SimpMessageType.CONNECT_ACK, null, "sender-session", sender), sender));
        registry.onApplicationEvent(new SessionConnectedEvent(this,
                queueMessage(SimpMessageType.CONNECT_ACK, null, "receiver-session", receiver), receiver));

        var senderSubscription = resolver.resolveDestination(queueMessage(
                SimpMessageType.SUBSCRIBE, "/user/queue/messages", "sender-session", sender));
        var receiverSubscription = resolver.resolveDestination(queueMessage(
                SimpMessageType.SUBSCRIBE, "/user/queue/messages", "receiver-session", receiver));
        assertEquals(Set.of("/queue/messages-usersender-session"), senderSubscription.getTargetDestinations());
        assertEquals(Set.of("/queue/messages-userreceiver-session"), receiverSubscription.getTargetDestinations());

        var toSender = resolver.resolveDestination(queueMessage(SimpMessageType.MESSAGE, "/user/10/queue/messages", null, null));
        var toReceiver = resolver.resolveDestination(queueMessage(SimpMessageType.MESSAGE, "/user/20/queue/messages", null, null));
        assertEquals(senderSubscription.getTargetDestinations(), toSender.getTargetDestinations());
        assertEquals(receiverSubscription.getTargetDestinations(), toReceiver.getTargetDestinations());
    }

    @Test
    void clientsCannotSubscribeToPhysicalQueuesOrPublishBrokerMessages() {
        var interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService);
        var user = authenticate("sender", 10L);

        assertDoesNotThrow(() -> interceptor.preSend(stompMessage(StompCommand.SUBSCRIBE, "/user/queue/messages", user), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(stompMessage(StompCommand.SUBSCRIBE, "/topic/conv.20.typing", user), null));
        assertDoesNotThrow(() -> interceptor.preSend(stompMessage(StompCommand.SEND, "/app/chat.send", user), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(stompMessage(StompCommand.SUBSCRIBE, "/queue/**", user), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(stompMessage(StompCommand.SUBSCRIBE, "/user/20/queue/messages", user), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(stompMessage(StompCommand.SEND, "/user/20/queue/messages", user), null));
    }

    private static Message<byte[]> stompMessage(StompCommand command, String destination, Principal user) {
        var accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Principal authenticate(String token, long userId) {
        String username = token + "@sqb.edu.vn";
        var userDetails = User.withUsername(username).password("unused").roles("STUDENT").build();
        var payload = JwtPayload.builder()
                .username(username)
                .userId(userId)
                .tokenType(TokenType.ACCESS)
                .accountState(AccountState.ACTIVE)
                .build();
        when(jwtService.decodeToken(token)).thenReturn(payload);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValid(payload, userDetails)).thenReturn(true);

        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        new WebSocketAuthInterceptor(jwtService, userDetailsService).preSend(message, null);
        assertEquals(userId, JwtPayload.getUserId(accessor.getUser()));
        return accessor.getUser();
    }

    private static Message<byte[]> queueMessage(SimpMessageType type, String destination, String session, Principal user) {
        var accessor = SimpMessageHeaderAccessor.create(type);
        if (destination != null) accessor.setDestination(destination);
        if (session != null) accessor.setSessionId(session);
        if (user != null) accessor.setUser(user);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
