package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.event.AccountDeletedEvent;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountWebSocketSessions implements ChannelInterceptor {
    private final JwtService jwt;
    private final UserDetailsService users;

    public record AccountSuspended(Long userId) {}

    private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();
    private final Map<String, JwtPayload> accounts = new ConcurrentHashMap<>();

    public WebSocketHandler decorate(WebSocketHandler delegate) {
        return new WebSocketHandlerDecorator(delegate) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sockets.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                sockets.remove(session.getId());
                accounts.remove(session.getId());
                super.afterConnectionClosed(session, status);
            }
        };
    }

    @EventListener
    public void connected(SessionConnectedEvent event) {
        String socketId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        JwtPayload payload = JwtPayload.fromPrincipal(event.getUser());
        if (socketId != null && payload != null && payload.getUserId() != null && sockets.containsKey(socketId)) {
            accounts.put(socketId, payload);
            if (!isCurrent(payload)) close(sockets.get(socketId));
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        accounts.remove(event.getSessionId());
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
        String socketId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        JwtPayload payload = socketId == null ? null : accounts.get(socketId);
        if (payload != null && !isCurrent(payload)) {
            close(sockets.get(socketId));
            return null;
        }
        return message;
    }

    private boolean isCurrent(JwtPayload payload) {
        try {
            return jwt.isTokenValid(payload, users.loadUserByUsername(payload.getUsername()));
        } catch (RuntimeException failure) {
            return false;
        }
    }

    @TransactionalEventListener
    public void onAccountsDeleted(AccountDeletedEvent event) {
        disconnect(new HashSet<>(event.userIds()));
    }

    @TransactionalEventListener
    public void onAccountSuspended(AccountSuspended event) {
        disconnect(Set.of(event.userId()));
    }

    private void disconnect(Set<Long> accountIds) {
        accounts.forEach((socketId, payload) -> {
            if (accountIds.contains(payload.getUserId())) close(sockets.get(socketId));
        });
    }

    private void close(WebSocketSession socket) {
        if (socket == null) return;
        try {
            socket.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException failure) {
            log.warn("Could not close revoked WebSocket session {}", socket.getId(), failure);
        }
    }
}
