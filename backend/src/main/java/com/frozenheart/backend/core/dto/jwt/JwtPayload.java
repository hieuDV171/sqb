package com.frozenheart.backend.core.dto.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;

import lombok.*;

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

    public static JwtPayload getCurrentUserPayload() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getDetails() instanceof JwtPayload)) {
            log.error("[JwtPayLoad]: {}", ResponseCode.ACCESS_DENIED.getMessage());
            throw new AppException(ResponseCode.ACCESS_DENIED);
        }

        return (JwtPayload) authentication.getDetails();
    }
}
