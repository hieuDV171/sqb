package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.GlobalResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void handle(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean onboarding = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ONBOARDING".equals(authority.getAuthority()));
        ResponseCode responseCode = onboarding
                ? ResponseCode.ACCOUNT_ONBOARDING_REQUIRED
                : ResponseCode.ACCESS_DENIED;

        GlobalResponse<Void> errorResponse = GlobalResponse.error(responseCode);

        jsonMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
