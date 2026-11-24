package com.frozenheart.backend.core.util;

import java.util.Arrays;
import java.util.Objects;

public final class ImageSignature {
    public static final int HEADER_LENGTH = 12;
    private static final byte[] PNG_HEADER = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private ImageSignature() {
    }

    public static boolean matches(byte[] bytes, String contentType) {
        return bytes.length >= HEADER_LENGTH && switch (Objects.toString(contentType, "")) {
            case "image/png" -> Arrays.equals(bytes, 0, PNG_HEADER.length, PNG_HEADER, 0, PNG_HEADER.length);
            case "image/jpeg" -> bytes[0] == (byte) 255 && bytes[1] == (byte) 216 && bytes[2] == (byte) 255;
            case "image/gif" -> bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                    && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "image/webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }
}
