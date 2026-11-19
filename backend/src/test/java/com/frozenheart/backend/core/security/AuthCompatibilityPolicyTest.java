package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.config.OpenApiConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthCompatibilityPolicyTest {
    @Test void swaggerReportsEnabledCompatibilityAndHowToDisableIt() {
        var policy = new AuthCompatibilityPolicy(true);
        String description = new OpenApiConfig(policy).customOpenAPI().getInfo().getDescription();
        assertTrue(policy.isLegacyJsonEnabled());
        assertTrue(description.startsWith("> ⚠️ **CHẾ ĐỘ TƯƠNG THÍCH RT JSON TẠM THỜI: ĐANG BẬT."));
        assertTrue(description.contains("AUTH_LEGACY_JSON_ENABLED=false"));
        assertTrue(description.contains("LEGACY_JSON"));
    }

    @Test void swaggerReportsStrictModeWhenCompatibilityIsDisabled() {
        var policy = new AuthCompatibilityPolicy(false);
        String description = new OpenApiConfig(policy).customOpenAPI().getInfo().getDescription();
        assertFalse(policy.isLegacyJsonEnabled());
        assertTrue(description.startsWith("> **CHẾ ĐỘ TƯƠNG THÍCH RT JSON TẠM THỜI: ĐÃ TẮT."));
    }
}
