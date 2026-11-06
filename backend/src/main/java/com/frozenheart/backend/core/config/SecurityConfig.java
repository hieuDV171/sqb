package com.frozenheart.backend.core.config;

import com.frozenheart.backend.core.config.filter.JwtAuthenticationFilter;
import com.frozenheart.backend.core.config.filter.MdcLoggingFilter;
import com.frozenheart.backend.core.security.CustomAccessDeniedHandler;
import com.frozenheart.backend.core.security.CustomAuthenticationEntryPoint;
import com.frozenheart.backend.core.security.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MdcLoggingFilter mdcLoggingFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            MdcLoggingFilter mdcLoggingFilter,
            CustomUserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.mdcLoggingFilter = mdcLoggingFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/docs/**", "/docs", "/api-docs/**").permitAll()

                        // Chỉ health/info được expose trong application.yaml; không còn exporter monitoring.
                        .requestMatchers("/actuator/**").permitAll()

                        .requestMatchers(
                                "/auth/email-verification/request",
                                "/auth/email-verification/confirm",
                                "/auth/change-password",
                                "/auth/logout"
                        ).authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/lecturer/**").hasAnyRole("LECTURER", "ADMIN")

                        .anyRequest().access((authentication, _) -> {
                            var current = authentication.get();

                            if (current == null || !current.isAuthenticated() || current instanceof AnonymousAuthenticationToken) {
                                return new AuthorizationDecision(false);
                            }

                            var authorities = current.getAuthorities();

                            boolean onboarding = authorities.stream().anyMatch(authority -> "ROLE_ONBOARDING".equals(authority.getAuthority()));

                            return new AuthorizationDecision(!onboarding);

                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mdcLoggingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
