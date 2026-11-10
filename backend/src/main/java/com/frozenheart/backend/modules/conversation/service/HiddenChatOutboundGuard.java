package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class HiddenChatOutboundGuard implements ChannelInterceptor {

    public static final String HIDDEN_CHAT_HEADER = "sqb-hidden-chat";

    private final HiddenChatAccess hiddenChatAccess;
    private final Map<String, JwtPayload> sessions = new ConcurrentHashMap<>();

    @EventListener
    public void connected(SessionConnectedEvent event) {
        JwtPayload session = JwtPayload.fromPrincipal(event.getUser());
        String socketId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (session != null && socketId != null) {
            sessions.put(socketId, session);
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (NativeMessageHeaderAccessor.getFirstNativeHeader(HIDDEN_CHAT_HEADER, message.getHeaders()) == null) {
            return message;
        }
        String socketId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        try {
            return socketId != null && hiddenChatAccess.isUnlocked(sessions.get(socketId)) ? message : null;
        } catch (DataAccessException unavailable) {
            return null;
        }
    }
}
