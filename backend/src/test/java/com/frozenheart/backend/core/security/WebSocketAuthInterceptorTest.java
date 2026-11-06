package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;

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
}
