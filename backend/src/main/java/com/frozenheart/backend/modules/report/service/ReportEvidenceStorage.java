package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.Http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportEvidenceStorage {
    public static final String LEGACY_OWNER_PREFIX = "sqb:report:legacy-owner:";
    private static final String STAGED = "sqb:report:evidence:staged";
    private static final String UPLOAD_LIMIT = "sqb:report:evidence:upload-limit:";
    private static final String PREFIX = "report-evidence/";
    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final Duration ATTACH_WINDOW = Duration.ofHours(23);
    private static final Duration CLEANUP_AGE = Duration.ofDays(1);
    private static final RedisScript<Long> RECORD_UPLOAD = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], 3600) end
            return count
            """, Long.class);

    private final R2Properties properties;
    private final MinioClient storage;
    private final RedisTemplate<String, String> redisTemplate;
    private final StringRedisTemplate strings;
    private final MediaService mediaService;
    private final ReportRepository reports;

    public String upload(Long ownerId, MultipartFile file) {
        checkUploadLimit(ownerId);
        try (InputStream input = file.getInputStream()) {
            return stage(ownerId, input, file.getSize(), file.getContentType());
        } catch (AppException failure) {
            throw failure;
        } catch (Exception failure) {
            throw unavailable(failure);
        }
    }

    public String attach(Long ownerId, List<String> keys, List<String> legacyUrls) {
        boolean hasKeys = keys != null && !keys.isEmpty();
        boolean hasUrls = legacyUrls != null && !legacyUrls.isEmpty();
        if (hasKeys && hasUrls) throw invalidEvidence();
        if (!hasKeys && !hasUrls) return null;

        String key = hasKeys ? keys.getFirst() : importLegacy(ownerId, legacyUrls.getFirst(), true);
        return attachStaged(ownerId, key);
    }

    String attachHistorical(Long ownerId, String url) {
        return attachStaged(ownerId, importLegacy(ownerId, url, false));
    }

    private String attachStaged(Long ownerId, String key) {
        Double stagedAt = redisTemplate.opsForZSet().score(STAGED, key);
        if (!key.matches(PREFIX + ownerId + "/[a-f0-9-]{36}") || stagedAt == null
                || stagedAt < Instant.now().minus(ATTACH_WINDOW).toEpochMilli()
                || reports.existsByPrivateEvidenceKey(key)) {
            throw invalidEvidence();
        }
        try {
            var object = storage.statObject(StatObjectArgs.builder().bucket(bucket()).object(key).build());
            if (object.size() <= 0 || object.size() > MAX_SIZE) throw invalidEvidence();
        } catch (AppException failure) {
            throw failure;
        } catch (ErrorResponseException failure) {
            if ("NoSuchKey".equals(failure.errorResponse().code())) throw invalidEvidence();
            throw unavailable(failure);
        } catch (Exception failure) {
            throw unavailable(failure);
        }
        mediaService.confirmMediaPermanent(List.of(key));
        // Keep the private staging entry until commit so rollback leaves the image eligible for cleanup.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    redisTemplate.opsForZSet().remove(STAGED, key);
                } catch (RuntimeException failure) {
                    log.warn("Could not clear staging entry for attached report evidence", failure);
                }
            }
        });
        return key;
    }

    public String readUrl(String key) {
        try {
            return storage.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                    .bucket(bucket()).object(key).expiry(120).build());
        } catch (AppException failure) {
            throw failure;
        } catch (Exception failure) {
            throw unavailable(failure);
        }
    }

    public void delete(String key) {
        try {
            storage.removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(key).build());
        } catch (AppException failure) {
            throw failure;
        } catch (Exception failure) {
            throw unavailable(failure);
        }
    }

    public void cleanStaging() {
        Set<String> keys = redisTemplate.opsForZSet().rangeByScore(STAGED, 0,
                Instant.now().minus(CLEANUP_AGE).toEpochMilli(), 0, 100);
        if (keys == null || keys.isEmpty()) return;
        Set<String> attached = reports.findAttachedEvidenceKeys(keys);
        for (String key : keys) {
            try {
                if (!attached.contains(key)) delete(key);
                redisTemplate.opsForZSet().remove(STAGED, key);
            } catch (RuntimeException failure) {
                log.warn("Could not clean staged report evidence", failure);
            }
        }
    }

    private String importLegacy(Long ownerId, String url, boolean requireUploadOwner) {
        String key = mediaService.extractObjectKey(url);
        if (key == null || !key.matches("posts/[0-9]{4}/[0-9]{2}/[a-f0-9-]{36}\\.[A-Za-z0-9]{1,5}")
                || !Objects.equals(url, mediaService.buildPublicUrl(key))) {
            throw invalidEvidence();
        }
        if (requireUploadOwner && !Objects.equals(ownerId.toString(),
                redisTemplate.opsForValue().get(LEGACY_OWNER_PREFIX + key))) {
            throw invalidEvidence();
        }
        return copyFromPublic(ownerId, key);
    }

    private String copyFromPublic(Long ownerId, String key) {
        try {
            var metadata = storage.statObject(StatObjectArgs.builder().bucket(properties.getBucketName())
                    .object(key).build());
            try (InputStream input = storage.getObject(GetObjectArgs.builder().bucket(properties.getBucketName())
                    .object(key).build())) {
                return stage(ownerId, input, metadata.size(), metadata.contentType());
            }
        } catch (AppException failure) {
            throw failure;
        } catch (Exception failure) {
            throw unavailable(failure);
        }
    }

    private String stage(Long ownerId, InputStream input, long size, String contentType) throws Exception {
        if (size <= 0 || size > MAX_SIZE) throw invalidEvidence();
        byte[] bytes = input.readNBytes((int) MAX_SIZE + 1);
        if (bytes.length != size || !isImage(bytes, contentType)) throw invalidEvidence();
        String bucket = bucket();
        String key = PREFIX + ownerId + "/" + UUID.randomUUID();
        redisTemplate.opsForZSet().add(STAGED, key, Instant.now().toEpochMilli());
        storage.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L).contentType(contentType)
                .headers(java.util.Map.of("Cache-Control", "private, no-store")).build());
        return key;
    }

    static boolean isImage(byte[] bytes, String contentType) {
        return bytes.length >= 12 && switch (Objects.toString(contentType, "")) {
            case "image/png" -> Arrays.equals(Arrays.copyOf(bytes, 8),
                    new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            case "image/jpeg" -> bytes[0] == (byte) 255 && bytes[1] == (byte) 216 && bytes[2] == (byte) 255;
            case "image/gif" -> bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                    && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "image/webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private String bucket() {
        String name = properties.getReportEvidenceBucket();
        if (name == null || name.isBlank() || name.equals(properties.getBucketName())) {
            throw new AppException(ResponseCode.REPORT_STORAGE_UNAVAILABLE, "Kho ảnh bằng chứng riêng tư chưa được cấu hình.");
        }
        return name;
    }

    private void checkUploadLimit(Long ownerId) {
        String key = UPLOAD_LIMIT + ownerId + ":" + Instant.now().getEpochSecond() / 3600;
        Long count = strings.execute(RECORD_UPLOAD, List.of(key));
        if (count == null || count > 20) {
            throw new AppException(ResponseCode.REPORT_RATE_LIMITED, "Bạn đã tải nhiều ảnh bằng chứng. Hãy thử lại sau.");
        }
    }

    private static AppException invalidEvidence() {
        return new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Ảnh bằng chứng phải thuộc phiên tải lên của bạn, là JPG, PNG, WebP hoặc GIF và tối đa 5 MB.");
    }

    private static AppException unavailable(Exception cause) {
        log.warn("Report evidence storage operation failed", cause);
        return new AppException(ResponseCode.REPORT_STORAGE_UNAVAILABLE, "Kho ảnh bằng chứng tạm thời không khả dụng.");
    }
}
