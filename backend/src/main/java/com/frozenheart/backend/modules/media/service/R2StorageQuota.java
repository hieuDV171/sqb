package com.frozenheart.backend.modules.media.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.service.impl.R2AccountUsage;
import com.frozenheart.backend.modules.media.service.impl.R2AccountUsage.ObjectRef;
import com.frozenheart.backend.modules.media.service.impl.R2MultipartClient;
import io.minio.AbortMultipartUploadArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2StorageQuota {
    private static final Duration MAX_SNAPSHOT_AGE = Duration.ofMinutes(15);
    private static final Duration PUT_GRACE = Duration.ofMinutes(15);
    private static final Duration DIRECT_RECOVERY = Duration.ofHours(2);
    private static final String PREFIX = "sqb:r2:quota:";

    private final R2Properties properties;
    private final R2AccountUsage accountUsage;
    private final R2MultipartClient multipart;
    private final MinioClient storage;
    private final StringRedisTemplate redis;
    private final RedissonClient redisson;
    private final JsonMapper mapper;

    public void putObject(PutObjectArgs args) throws Exception {
        if (!properties.isStorageQuotaEnabled()) {
            storage.putObject(args);
            return;
        }
        Long size = args.objectSize();
        if (size == null || size < 0) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Cần xác định dung lượng trước khi ghi tệp.");
        }
        String reservationId = UUID.randomUUID().toString();
        var hold = new Hold(args.bucket(), args.object(), size, Instant.now().plus(DIRECT_RECOVERY).toEpochMilli(),
                Kind.DIRECT, null, 0);
        reserve(Map.of(reservationId, hold));
        boolean written = false;
        try {
            storage.putObject(args);
            written = true;
        } finally {
            settle(reservationId, written ? Instant.now() : Instant.now().plus(PUT_GRACE));
        }
    }

    public void reservePresigned(String bucket, List<SizedUpload> uploads, Instant linkExpiry) {
        if (!properties.isStorageQuotaEnabled()) return;
        long recoverAt = linkExpiry.plus(PUT_GRACE).toEpochMilli();
        var holds = new HashMap<String, Hold>();
        for (SizedUpload upload : uploads) {
            var hold = new Hold(bucket, upload.key(), upload.size(), recoverAt, Kind.PRESIGNED, null, 0);
            holds.put(objectId(bucket, upload.key()), hold);
        }
        reserve(holds);
    }

    public void releaseUnissuedPresigned(String bucket, List<SizedUpload> uploads) {
        if (!properties.isStorageQuotaEnabled()) return;
        try {
            locked(state -> {
                uploads.forEach(upload -> state.holds().remove(objectId(bucket, upload.key())));
                save(state);
                return null;
            });
        } catch (RuntimeException failure) {
            log.warn("Could not release unissued R2 links. Their reservations will expire normally.");
        }
    }

    public void reserveMultipart(String bucket, String key, long size, long expiresAt, String uploadId) {
        if (!properties.isStorageQuotaEnabled()) return;
        reserve(Map.of(objectId(bucket, key), new Hold(bucket, key, size, expiresAt, Kind.MULTIPART, uploadId, 0)));
    }

    public void registerMultipart(String bucket, String key, String uploadId) {
        if (!properties.isStorageQuotaEnabled()) return;
        String id = objectId(bucket, key);
        locked(state -> {
            Hold hold = state.holds().get(id);
            if (hold == null || hold.kind() != Kind.MULTIPART) throw unavailable();
            state.holds().put(id, hold.withUploadId(uploadId));
            save(state);
            return null;
        });
    }

    public void finishMultipart(String bucket, String key) {
        if (properties.isStorageQuotaEnabled()) settle(objectId(bucket, key), Instant.now());
    }

    private void reserve(Map<String, Hold> additions) {
        locked(state -> {
            var pending = new HashMap<>(additions);
            pending.keySet().removeAll(state.holds().keySet());
            if (pending.isEmpty()) return null;
            if (state.observedAt() <= 0 || Instant.now().minus(MAX_SNAPSHOT_AGE).toEpochMilli() > state.observedAt()) {
                throw unavailable();
            }
            long before = state.projectedBytes();
            long requested = pending.values().stream().mapToLong(Hold::size).sum();
            if (requested > properties.getStorageLimitBytes() - before) {
                throw new AppException(ResponseCode.STORAGE_QUOTA_EXCEEDED,
                        "Kho lưu trữ đã gần đầy. Không thể ghi thêm tệp mới.");
            }
            state.holds().putAll(pending);
            save(state);
            if (before < properties.getStorageWarningBytes() && state.projectedBytes() >= properties.getStorageWarningBytes()) {
                warn(state);
            }
            return null;
        });
    }

    private void settle(String id, Instant recoverAt) {
        try {
            locked(state -> {
                Hold hold = state.holds().get(id);
                if (hold != null) {
                    state.holds().put(id, hold.finished(recoverAt.toEpochMilli()));
                    save(state);
                }
                return null;
            });
        } catch (RuntimeException ex) {
            log.warn("Could not settle R2 reservation. It will remain counted until reconciliation.");
        }
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void reconcile() {
        if (!properties.isStorageQuotaEnabled()) return;
        try {
            var lock = redisson.getLock(redisKey() + ":reconcile");
            if (!lock.tryLock()) return;
            try {
                State before = locked(Function.identity());
                long startedAt = Instant.now().toEpochMilli();
                var expiredMultipart = new HashSet<String>();
                before.holds().forEach((id, hold) -> {
                    if (hold.kind() == Kind.MULTIPART && hold.recoverAt() <= startedAt && abortExpired(hold)) {
                        expiredMultipart.add(id);
                    }
                });
                var tracked = before.holds().values().stream().filter(hold -> hold.kind() == Kind.PRESIGNED)
                        .map(Hold::ref).collect(Collectors.toSet());
                var observed = accountUsage.read(tracked);
                locked(current -> {
                    var refreshed = applyInventory(before, current, startedAt, expiredMultipart, observed);
                    save(refreshed);
                    if (current.observedAt() == 0) {
                        log.info("R2 quota initialized: observedBytes={}, limitBytes={}",
                                refreshed.usedBytes(), properties.getStorageLimitBytes());
                    }
                    if (refreshed.projectedBytes() >= properties.getStorageWarningBytes()) warn(refreshed);
                    return null;
                });
            } finally {
                lock.unlock();
            }
        } catch (RuntimeException failure) {
            if (failure instanceof IllegalStateException) {
                log.warn("R2 quota reconciliation failed: {}", failure.getMessage());
            } else if (failure instanceof org.springframework.web.client.RestClientResponseException response) {
                log.warn("R2 quota reconciliation failed: Cloudflare returned HTTP {}", response.getStatusCode().value());
            } else {
                log.warn("R2 quota reconciliation failed: {}", failure.getClass().getSimpleName());
            }
        }
    }

    static State applyInventory(State before, State current, long startedAt, Set<String> expiredMultipart,
                                R2AccountUsage.Usage observed) {
        var holds = new HashMap<>(current.holds());
        holds.entrySet().removeIf(entry -> {
            Hold hold = entry.getValue();
            if (!hold.equals(before.holds().get(entry.getKey()))) return false;
            return hold.kind() == Kind.MULTIPART ? expiredMultipart.contains(entry.getKey()) : hold.recoverAt() <= startedAt;
        });
        holds.replaceAll((id, hold) -> {
            long covered = hold.kind() == Kind.PRESIGNED && hold.equals(before.holds().get(id))
                    ? Math.min(hold.size(), observed.observed().getOrDefault(hold.ref(), 0L)) : 0;
            return hold.withCovered(covered);
        });
        return new State(observed.bytes(), startedAt, holds);
    }

    private boolean abortExpired(Hold hold) {
        if (hold.uploadId() == null) return true; // No part links were issued.
        try {
            multipart.abortMultipartUpload(AbortMultipartUploadArgs.builder()
                    .bucket(hold.bucket()).object(hold.key()).uploadId(hold.uploadId()).build()).join();
            return true;
        } catch (CompletionException failure) {
            if (failure.getCause() instanceof ErrorResponseException error && error.errorResponse() != null
                    && "NoSuchUpload".equals(error.errorResponse().code())) return true;
            log.warn("Could not abort an expired R2 multipart upload. Retaining its reservation.");
            return false;
        }
    }

    private <T> T locked(Function<State, T> operation) {
        try {
            var lock = redisson.getLock(redisKey() + ":lock");
            lock.lock();
            try {
                String data = redis.opsForValue().get(redisKey());
                return operation.apply(data == null ? new State(0, 0, new HashMap<>()) : mapper.readValue(data, State.class));
            } finally {
                lock.unlock();
            }
        } catch (AppException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            log.warn("Could not access R2 quota state: {}", failure.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private void save(State state) {
        redis.opsForValue().set(redisKey(), mapper.writeValueAsString(state));
    }

    private static AppException unavailable() {
        return new AppException(ResponseCode.STORAGE_QUOTA_UNAVAILABLE,
                "Chưa thể kiểm tra dung lượng kho lưu trữ. Vui lòng thử lại sau.");
    }

    private void warn(State state) {
        log.warn("R2 storage near quota: observedBytes={}, projectedBytes={}, limitBytes={}",
                state.usedBytes(), state.projectedBytes(), properties.getStorageLimitBytes());
    }

    private String redisKey() {
        return PREFIX + properties.getAccountId();
    }

    private static String objectId(String bucket, String key) {
        return bucket + "/" + key;
    }

    public record SizedUpload(String key, long size) {}
    enum Kind { DIRECT, SETTLED, PRESIGNED, MULTIPART }

    record Hold(String bucket, String key, long size, long recoverAt, Kind kind, String uploadId, long covered) {
        ObjectRef ref() {
            return new ObjectRef(bucket, key);
        }

        Hold withUploadId(String id) {
            return new Hold(bucket, key, size, recoverAt, kind, id, covered);
        }

        Hold withCovered(long bytes) {
            return new Hold(bucket, key, size, recoverAt, kind, uploadId, bytes);
        }

        Hold finished(long at) {
            return new Hold(bucket, key, size, at, Kind.SETTLED, null, 0);
        }
    }

    record State(long usedBytes, long observedAt, Map<String, Hold> holds) {
        long projectedBytes() {
            return usedBytes + holds.values().stream().mapToLong(hold -> hold.size() - hold.covered()).sum();
        }
    }
}
