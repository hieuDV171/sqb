package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccountValidationTest {
    @Test
    void suspensionAndEpochRejectOldTokensEvenAfterEarlyRelease() {
        var jwt = new JwtService(null);
        var user = User.builder().id(1L).email("student@example.edu").active(true).build();
        var current = JwtPayload.builder().userId(1L).username(user.getEmail()).sessionEpoch(1L).build();
        var legacy = JwtPayload.builder().userId(1L).username(user.getEmail()).build();
        var details = new CustomUserDetails(user);

        user.setSessionEpoch(1L);
        user.setSuspendedUntil(Instant.now().plusSeconds(60));
        assertThat(jwt.isTokenValid(current, details)).isFalse();
        user.setSuspendedUntil(null);
        assertThat(jwt.isTokenValid(legacy, details)).isFalse();
        assertThat(jwt.isTokenValid(current, details)).isTrue();
    }


    @Test
    void tokensMustStillBelongToTheActiveAccount() {
        var jwtService = new JwtService(null);
        var user = User.builder().id(1L).email("student@example.edu").active(true).build();
        var details = new CustomUserDetails(user);
        var payload = JwtPayload.builder().userId(1L).username(user.getEmail()).build();

        assertThat(jwtService.isTokenValid(payload, details)).isTrue();

        user.setActive(false);
        assertThat(jwtService.isTokenValid(payload, details)).isFalse();

        user.setActive(true);
        user.setId(2L);
        assertThat(jwtService.isTokenValid(payload, details)).isFalse();
    }
}
