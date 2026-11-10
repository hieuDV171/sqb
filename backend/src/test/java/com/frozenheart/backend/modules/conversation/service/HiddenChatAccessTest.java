package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.JwtService;
import com.frozenheart.backend.core.security.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HiddenChatAccessTest {

    @Mock RedisTemplate<String, String> redis;
    @Mock StringRedisTemplate strings;
    @Mock ValueOperations<String, String> values;
    @Mock JwtService jwt;

    @Test
    void grantBelongsToOneLoginAndStopsWorkingAfterReloginOrRevocation() {
        var access = new HiddenChatAccess(redis, strings, jwt);
        JwtPayload first = session("login-a");
        JwtPayload second = session("login-b");
        String refreshKey = "rt:student@sqb.edu.vn:browser";
        String grantKey = "hidden-chat:grant:10:login-a";
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(refreshKey)).thenReturn("refresh-a", "rotated-a", "refresh-b", null);
        when(jwt.decodeToken("refresh-a")).thenReturn(refresh("login-a"));
        when(jwt.decodeToken("rotated-a")).thenReturn(refresh("login-a"));
        when(jwt.decodeToken("refresh-b")).thenReturn(refresh("login-b"));
        when(redis.hasKey(grantKey)).thenReturn(true, true, true, false);

        access.unlock(first);
        verify(values).set(grantKey, "unlocked", Duration.ofMinutes(15));
        assertTrue(access.isUnlocked(first)); // A rotated refresh token keeps the login identity.
        assertFalse(access.isUnlocked(second));
        assertFalse(access.isUnlocked(first)); // Same device has logged in again.
        assertFalse(access.isUnlocked(first)); // Remote logout removed its refresh token.
        assertThrows(AppException.class, () -> access.requireUnlocked(first)); // Grant expired.
        access.lock(first);
        verify(redis).delete(grantKey);
    }

    @Test
    void brokerFiltersHiddenEventsForEachSocketEvenWhenTheAccountIsTheSame() {
        var access = new HiddenChatAccess(redis, strings, jwt);
        var guard = new HiddenChatOutboundGuard(access);
        connect(guard, "socket-a", session("login-a"));
        connect(guard, "socket-b", session("login-b"));
        when(redis.hasKey("hidden-chat:grant:10:login-a")).thenReturn(true, false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("rt:student@sqb.edu.vn:browser")).thenReturn("refresh-a");
        when(jwt.decodeToken("refresh-a")).thenReturn(refresh("login-a"));

        Message<byte[]> hiddenA = frame("socket-a", true);
        assertSame(hiddenA, guard.preSend(hiddenA, null));
        assertNull(guard.preSend(frame("socket-b", true), null));
        assertNull(guard.preSend(hiddenA, null)); // Existing subscription cannot outlive the grant.
        Message<byte[]> ordinaryB = frame("socket-b", false);
        assertSame(ordinaryB, guard.preSend(ordinaryB, null));
    }

    private static JwtPayload session(String id) {
        return JwtPayload.builder().userId(10L).username("student@sqb.edu.vn")
                .deviceId("browser").sessionId(id).tokenType(TokenType.ACCESS).build();
    }

    private static JwtPayload refresh(String id) {
        return JwtPayload.builder().sessionId(id).tokenType(TokenType.REFRESH).build();
    }

    private static void connect(HiddenChatOutboundGuard guard, String socketId, JwtPayload session) {
        var principal = UsernamePasswordAuthenticationToken.authenticated("10", null, List.of());
        principal.setDetails(session);
        guard.connected(new SessionConnectedEvent(guard, frame(socketId, false), principal));
    }

    private static Message<byte[]> frame(String socketId, boolean hidden) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(socketId);
        if (hidden) {
            headers.setNativeHeader(HiddenChatOutboundGuard.HIDDEN_CHAT_HEADER, "true");
        }
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
