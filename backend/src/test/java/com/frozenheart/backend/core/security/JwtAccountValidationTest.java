package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccountValidationTest {

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
