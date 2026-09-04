package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                authHeader = accessor.getFirstNativeHeader("token");
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            if (authHeader == null || authHeader.isBlank()) {
                log.error("[WebSocketAuthInterceptor] WebSocket CONNECT rejected: Missing Authorization header");
                throw new MessageDeliveryException("Unauthorized: Missing Authorization header");
            }

            try {
                JwtPayload jwtPayload = jwtService.decodeToken(authHeader);
                String username = jwtPayload.getUsername();

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(jwtPayload, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(jwtPayload);
                        accessor.setUser(authentication);
                        log.debug("[WebSocketAuthInterceptor] WebSocket authenticated user: {} (ID: {})", username, jwtPayload.getUserId());
                    } else {
                        log.error("[WebSocketAuthInterceptor] WebSocket CONNECT rejected: Token is expired or invalid for user {}", username);
                        throw new MessageDeliveryException("Unauthorized: Token is expired or invalid");
                    }
                } else {
                    log.error("[WebSocketAuthInterceptor] WebSocket CONNECT rejected: Username is null");
                    throw new MessageDeliveryException("Unauthorized: Username is null");
                }
            } catch (MessageDeliveryException mde) {
                throw mde;
            } catch (Exception e) {
                log.error("[WebSocketAuthInterceptor] WebSocket JWT authentication failed: {}", e.getMessage());
                throw new MessageDeliveryException("Unauthorized: " + e.getMessage());
            }
        }

        return message;
    }
}
