package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.conversation.UserParticipant;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.JwtService;
import com.frozenheart.backend.core.security.TokenType;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HiddenChatAccess {

    private static final Duration UNLOCK_DURATION = Duration.ofMinutes(15);
    private static final RedisScript<Long> PIN_ATTEMPTS = RedisScript.of("""
            local attempts = redis.call('INCR', KEYS[1])
            if attempts == 1 then redis.call('EXPIRE', KEYS[1], 900) end
            return attempts
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtService jwtService;

    public void countPinAttempt(JwtPayload session) {
        Long attempts = stringRedisTemplate.execute(PIN_ATTEMPTS, List.of(attemptKey(session)));
        if (attempts == null || attempts > 5) {
            throw new AppException(ResponseCode.HIDDEN_CHAT_PIN_ATTEMPT_LIMIT,
                    "Bạn đã thử PIN quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }
    }

    public Instant unlock(JwtPayload session) {
        requireActiveSession(session);
        Instant expiresAt = Instant.now().plus(UNLOCK_DURATION);
        redisTemplate.opsForValue().set(grantKey(session), "unlocked", UNLOCK_DURATION);
        clearPinAttempts(session);
        return expiresAt;
    }

    public void clearPinAttempts(JwtPayload session) {
        stringRedisTemplate.delete(attemptKey(session));
    }

    public void requireActiveSession(JwtPayload session) {
        if (!isActiveSession(session)) {
            throw new AppException(ResponseCode.TOKEN_INVALID_OR_EXPIRED);
        }
    }

    public void lock(JwtPayload session) {
        if (hasSessionId(session)) {
            redisTemplate.delete(grantKey(session));
        }
    }

    public boolean isUnlocked(JwtPayload session) {
        return hasSessionId(session) && Boolean.TRUE.equals(redisTemplate.hasKey(grantKey(session)))
                && isActiveSession(session);
    }

    public void requireUnlocked(JwtPayload session) {
        if (!isUnlocked(session)) {
            throw new AppException(ResponseCode.HIDDEN_CHAT_LOCKED, "Hãy mở khóa kho ẩn bằng mã PIN");
        }
    }

    public void requireIfHidden(UserParticipant participant) {
        if (participant.getHiddenAt() != null) {
            requireUnlocked(JwtPayload.getCurrentUserPayload());
        }
    }

    private boolean isActiveSession(JwtPayload session) {
        if (!hasSessionId(session)) {
            return false;
        }
        String token = redisTemplate.opsForValue().get("rt:" + session.getUsername() + ":" + session.getDeviceId());
        if (token == null) {
            return false;
        }
        try {
            JwtPayload refresh = jwtService.decodeToken(token);
            return refresh.getTokenType() == TokenType.REFRESH && session.getSessionId().equals(refresh.getSessionId());
        } catch (JwtException | IllegalArgumentException invalidToken) {
            return false;
        }
    }

    private static boolean hasSessionId(JwtPayload session) {
        return session != null && session.getTokenType() == TokenType.ACCESS
                && session.getSessionId() != null && session.getUserId() != null;
    }

    private static String grantKey(JwtPayload session) {
        return "hidden-chat:grant:" + session.getUserId() + ":" + session.getSessionId();
    }

    private static String attemptKey(JwtPayload session) {
        return "hidden-chat:pin-attempts:" + session.getUserId();
    }
}
