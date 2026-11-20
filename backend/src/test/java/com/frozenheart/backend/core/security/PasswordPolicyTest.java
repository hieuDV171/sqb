package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.text.Normalizer;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.*;

class PasswordPolicyTest {
    @Test void vietnameseCaseCountsAsLettersAndCombiningAccentsAreNotSpecial() {
        assertThat(PasswordPolicy.requireValid("Đạihọc2026!AB")).isEqualTo("Đạihọc2026!AB");
        assertThatThrownBy(() -> PasswordPolicy.requireValid("Đạihọc2026ABC")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> PasswordPolicy.requireValid("Đạihọc2026AB\u0301")).isInstanceOf(AppException.class);
    }

    @Test void enforcesNormalizedUtf8BoundaryWithoutTrimmingOrTruncation() {
        String boundary = "Aa1!" + "ệ".repeat(22) + "ab"; // 4 + 66 + 2 = 72 bytes
        assertThat(PasswordPolicy.requireValid(boundary)).isEqualTo(boundary);
        assertThatThrownBy(() -> PasswordPolicy.requireValid(boundary + "a")).isInstanceOf(AppException.class);
        assertThat(PasswordPolicy.requireValid("  Abcdef1234!  ")).isEqualTo("  Abcdef1234!  ");
        assertThatThrownBy(() -> PasswordPolicy.requireValid("Abcdef1234!\n")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> PasswordPolicy.requireValid("Abcdef1234!\u200b")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> PasswordPolicy.requireValid("Aa1!short")).isInstanceOf(AppException.class);
    }

    @Test void canonicalPasswordsAndExistingRawHashesBothAuthenticate() {
        var encoder = new BCryptPasswordEncoder(4);
        String canonical = "Đạihọc2026!AB";
        String decomposed = Normalizer.normalize(canonical, Normalizer.Form.NFD);
        String newHash = encoder.encode(PasswordPolicy.requireValid(decomposed));
        assertThat(PasswordPolicy.matches(encoder, canonical, newHash)).isTrue();
        assertThat(PasswordPolicy.matches(encoder, decomposed, newHash)).isTrue();
        String legacyHash = encoder.encode(decomposed);
        assertThat(PasswordPolicy.matches(encoder, decomposed, legacyHash)).isTrue();
        String weakLegacyHash = encoder.encode("12345678");
        assertThat(PasswordPolicy.matches(encoder, "12345678", weakLegacyHash)).isTrue();
        assertThat(PasswordPolicy.matches(encoder, "x".repeat(73), newHash)).isFalse();
    }

    @Test void temporaryPasswordsAreDistinctAsciiAndAlwaysMeetAllGroups() {
        var generated = new HashSet<String>();
        for (int i = 0; i < 200; i++) {
            String password = PasswordPolicy.generateTemporary();
            assertThat(password).hasSize(16).matches("[\\x21-\\x7e]+");
            assertThat(PasswordPolicy.requireValid(password)).isEqualTo(password);
            assertThat(generated.add(password)).isTrue();
        }
    }
}
