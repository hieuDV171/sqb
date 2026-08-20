package com.frozenheart.backend.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.GlobalResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        GlobalResponse<Void> errorResponse = GlobalResponse.error(
                ResponseCode.TOKEN_INVALID_OR_EXPIRED,
                authException.getMessage() != null ? authException.getMessage() : ResponseCode.TOKEN_INVALID_OR_EXPIRED.getMessage()
        );

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
