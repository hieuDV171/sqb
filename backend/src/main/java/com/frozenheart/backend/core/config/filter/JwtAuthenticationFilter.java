package com.frozenheart.backend.core.config.filter;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.security.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            JwtPayload jwtPayload = jwtService.decodeToken(jwt);
            String username = jwtPayload.getUsername();

            if (username != null && authentication == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwtPayload, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // không đặt MK vào đây
                            userDetails.getAuthorities()
                    );

                    authenticationToken.setDetails(jwtPayload);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Bổ sung danh tính user vào MDC để tương quan log
                    if (jwtPayload.getUserId() != null) {
                        MDC.put("userId", String.valueOf(jwtPayload.getUserId()));
                    }
                    if (username != null) {
                        MDC.put("username", username);
                    }
                }
            }
        } catch (Exception _) {

        }

        filterChain.doFilter(request, response);

    }
}
