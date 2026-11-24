package com.frozenheart.backend.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageSignatureTest {
    @Test
    void rejectsImagesWithIncorrectOrUnsafeContentTypes() {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
        assertThat(ImageSignature.matches(png, "image/png")).isTrue();
        assertThat(ImageSignature.matches(png, "image/gif")).isFalse();
        assertThat(ImageSignature.matches(png, "text/html")).isFalse();
        assertThat(ImageSignature.matches(png, null)).isFalse();
        assertThat(ImageSignature.matches(new byte[0], "image/png")).isFalse();
    }

    @Test
    void requiresTheFullGifSignature() {
        byte[] gif87a = {'G', 'I', 'F', '8', '7', 'a', 0, 0, 0, 0, 0, 0};
        byte[] gif89a = {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0};
        byte[] invalid = {'G', 'I', 'F', '8', '8', 'a', 0, 0, 0, 0, 0, 0};
        assertThat(ImageSignature.matches(gif87a, "image/gif")).isTrue();
        assertThat(ImageSignature.matches(gif89a, "image/gif")).isTrue();
        assertThat(ImageSignature.matches(invalid, "image/gif")).isFalse();
    }
}
