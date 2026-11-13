package com.frozenheart.backend.core.dto.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.AccountState;
import com.frozenheart.backend.core.security.TokenType;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class JwtPayload {
    private String username;
    private Long userId;
    private String role;
    private String deviceId;
    private String sessionId;
    private TokenType tokenType;
    private AccountState accountState;
    private Instant issuedAt;

    public static JwtPayload getCurrentUserPayload() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getDetails() instanceof JwtPayload)) {
            log.error("[JwtPayLoad]: {}", ResponseCode.ACCESS_DENIED.getMessage());
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        return (JwtPayload) authentication.getDetails();
    }

    public static Long getUserId(java.security.Principal principal) {
        JwtPayload payload = fromPrincipal(principal);
        return payload == null ? null : payload.getUserId();
    }

    public static JwtPayload fromPrincipal(java.security.Principal principal) {
        if (principal instanceof Authentication auth && auth.getDetails() instanceof JwtPayload payload) {
            return payload;
        }
        return null;
    }
}
