package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.event.AccountDeletedEvent;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AccountWebSocketSessions {

    private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();
    private final Map<String, Long> accounts = new ConcurrentHashMap<>();

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
        Long userId = JwtPayload.getUserId(event.getUser());
        if (socketId != null && userId != null && sockets.containsKey(socketId)) {
            accounts.put(socketId, userId);
        }
    }

    @TransactionalEventListener
    public void onAccountsDeleted(AccountDeletedEvent event) {
        Set<Long> deletedIds = new HashSet<>(event.userIds());
        accounts.forEach((socketId, userId) -> {
            WebSocketSession socket = sockets.get(socketId);
            if (deletedIds.contains(userId) && socket != null) {
                try {
                    socket.close(CloseStatus.POLICY_VIOLATION);
                } catch (IOException e) {
                    log.warn("Could not close WebSocket session for deleted account {}", userId, e);
                }
            }
        });
    }
}
