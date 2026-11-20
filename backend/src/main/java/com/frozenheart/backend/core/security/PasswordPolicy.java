package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;

/** Account passwords only: legacy authentication must not enforce the new composition policy. */
public final class PasswordPolicy {
    public static final int MIN_CHARACTERS = 12;
    public static final int MAX_BYTES = 72;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String[] GROUPS = {"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz",
            "0123456789", "!@#$%^&*()-_=+[]{}:;,.?"};
    private static final String ALPHABET = String.join("", GROUPS);

    private PasswordPolicy() {}

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    public static String requireValid(String value) {
        if (value == null) throw invalid("Vui lòng nhập mật khẩu.");
        String password = normalize(value);
        if (password.codePointCount(0, password.length()) < MIN_CHARACTERS)
            throw invalid("Mật khẩu phải có ít nhất 12 ký tự.");
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw invalid("Mật khẩu không được vượt quá 72 byte UTF-8.");
        if (!password.codePoints().allMatch(PasswordPolicy::isAllowed))
            throw invalid("Mật khẩu không được chứa ký tự điều khiển hoặc ký tự ẩn.");
        if (!password.codePoints().anyMatch(cp -> Character.getType(cp) == Character.UPPERCASE_LETTER)
                || !password.codePoints().anyMatch(cp -> Character.getType(cp) == Character.LOWERCASE_LETTER)
                || !password.codePoints().anyMatch(cp -> cp >= '0' && cp <= '9')
                || !password.codePoints().anyMatch(PasswordPolicy::isSpecial))
            throw invalid("Mật khẩu phải có chữ hoa, chữ thường, chữ số (0–9) và ký tự đặc biệt.");
        return password;
    }

    private static boolean isAllowed(int cp) {
        int type = Character.getType(cp);
        return Character.isLetter(cp) || Character.isDigit(cp) || isSpecial(cp)
                || type == Character.LETTER_NUMBER || type == Character.OTHER_NUMBER
                || type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK || type == Character.SPACE_SEPARATOR;
    }

    private static boolean isSpecial(int cp) {
        return switch (Character.getType(cp)) {
            case Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
                 Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
                 Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION,
                 Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL,
                 Character.OTHER_SYMBOL -> true;
            default -> false;
        };
    }

    public static String generateTemporary() {
        char[] result = new char[16];
        for (int i = 0; i < GROUPS.length; i++) result[i] = GROUPS[i].charAt(RANDOM.nextInt(GROUPS[i].length()));
        for (int i = GROUPS.length; i < result.length; i++) result[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        for (int i = result.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char swap = result[i]; result[i] = result[j]; result[j] = swap;
        }
        return new String(result);
    }

    /** Try the original representation first so existing non-NFC hashes keep working. */
    public static boolean matches(PasswordEncoder encoder, String raw, String hash) {
        if (raw == null) return false;
        if (raw.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES && encoder.matches(raw, hash)) return true;
        String normalized = normalize(raw);
        return !normalized.equals(raw) && normalized.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES
                && encoder.matches(normalized, hash);
    }

    private static AppException invalid(String message) {
        return new AppException(ResponseCode.INVALID_PARAMETER_VALUE, message);
    }
}
