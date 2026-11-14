package com.frozenheart.backend.modules.report.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportEvidenceStorageTest {
    @Test
    void rejectsImagesWithIncorrectOrUnsafeContentTypes() {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
        assertThat(ReportEvidenceStorage.isImage(png, "image/png")).isTrue();
        assertThat(ReportEvidenceStorage.isImage(png, "image/gif")).isFalse();
        assertThat(ReportEvidenceStorage.isImage(png, "text/html")).isFalse();
        assertThat(ReportEvidenceStorage.isImage(new byte[0], "image/png")).isFalse();
    }
}
