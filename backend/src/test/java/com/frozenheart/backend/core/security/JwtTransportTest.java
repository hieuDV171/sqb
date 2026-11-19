package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.config.property.JwtProperties;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JwtTransportTest {
    @Test void signedTokensPreserveTransportAndLegacyTokensRemainUntyped() {
        var properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(new byte[64]));
        properties.setIssuer("test");
        properties.setAccessTokenExpiration(60_000);
        properties.setRefreshTokenExpiration(60_000);
        var service = new JwtService(properties);
        for (var type : AuthClientType.values()) {
            var refresh = service.generateRefreshToken("fixture@test", 7L, "device", "session", 1L, type);
            assertEquals(type, service.decodeToken(refresh).getAuthClientType());
            var access = service.generateAccessToken(Map.of("authClientType", type.name(), "tokenType", "ACCESS"), "fixture@test");
            assertEquals(type, service.decodeToken(access).getAuthClientType());
        }
        assertNull(service.decodeToken(service.generateRefreshToken("fixture@test", 7L, "device", "session")).getAuthClientType());
    }
}
