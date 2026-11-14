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

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Set<String> CHAT_COMMANDS = Set.of("/app/chat.send", "/app/chat.read", "/app/chat.typing");
    private static final Set<String> USER_SUBSCRIPTIONS = Set.of("/user/queue/messages", "/user/queue/notifications");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            checkClientDestination(accessor);
            if (accessor.getCommand() == StompCommand.SEND || accessor.getCommand() == StompCommand.SUBSCRIBE) {
                JwtPayload payload = JwtPayload.fromPrincipal(accessor.getUser());
                if (payload == null || !jwtService.isTokenValid(payload,
                        userDetailsService.loadUserByUsername(payload.getUsername()))) {
                    throw new MessageDeliveryException("Unauthorized: Account session was revoked");
                }
            }
        }

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
                Long userId = jwtPayload.getUserId();

                if (jwtPayload.getTokenType() != TokenType.ACCESS
                        || jwtPayload.getAccountState() != AccountState.ACTIVE) {
                    throw new MessageDeliveryException("Unauthorized: Account setup is incomplete");
                }

                if (username != null && userId != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(jwtPayload, userDetails)) {
                        // Spring routes /user destinations by Principal#getName. Chat addresses users by ID.
                        UsernamePasswordAuthenticationToken authentication =
                                UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, userDetails.getAuthorities());
                        authentication.setDetails(jwtPayload);
                        accessor.setUser(authentication);
                        log.debug("[WebSocketAuthInterceptor] WebSocket authenticated user: {} (ID: {})", username, jwtPayload.getUserId());
                    } else {
                        log.error("[WebSocketAuthInterceptor] WebSocket CONNECT rejected: Token is expired or invalid for user {}", username);
                        throw new MessageDeliveryException("Unauthorized: Token is expired or invalid");
                    }
                } else {
                    log.error("[WebSocketAuthInterceptor] WebSocket CONNECT rejected: Missing account identity");
                    throw new MessageDeliveryException("Unauthorized: Missing account identity");
                }
            } catch (MessageDeliveryException mde) {
                throw mde;
            } catch (Exception e) {
                log.warn("[WebSocketAuthInterceptor] WebSocket JWT authentication failed: {}", e.getMessage());
                throw new MessageDeliveryException("Unauthorized: " + e.getMessage());
            }
        }

        return message;
    }

    private static void checkClientDestination(StompHeaderAccessor accessor) {
        StompCommand command = accessor.getCommand();
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
            return;
        }

        String destination = accessor.getDestination();
        if (JwtPayload.getUserId(accessor.getUser()) == null || destination == null) {
            throw new MessageDeliveryException("Unauthorized: Missing STOMP identity or destination");
        }

        // Clients must not subscribe to physical broker queues or publish directly to them.
        boolean allowed = command == StompCommand.SEND
                ? CHAT_COMMANDS.contains(destination)
                : USER_SUBSCRIPTIONS.contains(destination);
        if (!allowed) {
            throw new MessageDeliveryException("Forbidden: Invalid STOMP destination");
        }
    }
}
