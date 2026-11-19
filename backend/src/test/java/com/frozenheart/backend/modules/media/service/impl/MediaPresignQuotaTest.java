package com.frozenheart.backend.modules.media.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.media.MediaPurpose;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest.PresignFileItem;
import com.frozenheart.backend.modules.media.service.R2StorageQuota;
import com.frozenheart.backend.modules.media.service.R2StorageQuota.SizedUpload;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MediaPresignQuotaTest {
    @Test
    @SuppressWarnings("unchecked")
    void rejectsADeclaredTenGigabyteFileBeforeQuotaReservationOrSigning() {
        var properties = new R2Properties();
        properties.setBucketName("sqb-public");
        var signer = mock(R2MultipartClient.class);
        var quota = mock(R2StorageQuota.class);
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        var service = new MediaServiceImpl(null, properties, redis, quota, signer);

        var error = assertThrows(AppException.class, () -> service.generatePresignedUrls(new PresignMediaRequest(List.of(
                new PresignFileItem("huge.pdf", 10_000_000_001L, "application/pdf", MediaPurpose.DOCUMENT)))));

        assertEquals(ResponseCode.INVALID_PARAMETER_VALUE, error.getResponseCode());
        verifyNoInteractions(quota, signer, redis);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsTheWholeRequestBeforeSigningAnyLink() {
        var properties = new R2Properties();
        properties.setBucketName("sqb-public");
        var signer = mock(R2MultipartClient.class);
        var quota = mock(R2StorageQuota.class);
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        var service = new MediaServiceImpl(null, properties, redis, quota, signer);
        doThrow(new AppException(ResponseCode.STORAGE_QUOTA_EXCEEDED)).when(quota)
                .reservePresigned(eq("sqb-public"),
                        argThat(files -> files.stream().map(SizedUpload::size).toList().equals(List.of(1L, 2L))),
                        any(Instant.class));

        var error = assertThrows(AppException.class, () -> service.generatePresignedUrls(new PresignMediaRequest(List.of(
                new PresignFileItem("a.webp", 1L, "image/webp", MediaPurpose.QUESTION),
                new PresignFileItem("b.webp", 2L, "image/webp", MediaPurpose.QUESTION)))));

        assertEquals(ResponseCode.STORAGE_QUOTA_EXCEEDED, error.getResponseCode());
        verifyNoInteractions(signer, redis);
    }
}
