package com.frozenheart.backend.modules.media.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.service.R2StorageQuota.Hold;
import com.frozenheart.backend.modules.media.service.R2StorageQuota.Kind;
import com.frozenheart.backend.modules.media.service.R2StorageQuota.SizedUpload;
import com.frozenheart.backend.modules.media.service.R2StorageQuota.State;
import com.frozenheart.backend.modules.media.service.impl.R2AccountUsage;
import com.frozenheart.backend.modules.media.service.impl.R2MultipartClient;
import io.minio.AbortMultipartUploadArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class R2StorageQuotaTest {
    private final R2Properties properties = new R2Properties();
    private final R2AccountUsage accountUsage = mock(R2AccountUsage.class);
    private final R2MultipartClient multipart = mock(R2MultipartClient.class);
    private final MinioClient storage = mock(MinioClient.class);
    private final AtomicReference<String> data = new AtomicReference<>();
    private final JsonMapper mapper = JsonMapper.builder().build();
    private R2StorageQuota quota;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties.setStorageQuotaEnabled(true);
        properties.setAccountId("fixture");
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> data.get());
        doAnswer(call -> {
            data.set(call.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString());
        var lock = new ReentrantLock();
        var redissonLock = mock(RLock.class);
        doAnswer(call -> { lock.lock(); return null; }).when(redissonLock).lock();
        doAnswer(call -> { lock.unlock(); return null; }).when(redissonLock).unlock();
        when(redissonLock.tryLock()).thenAnswer(call -> lock.tryLock());
        var redisson = mock(RedissonClient.class);
        when(redisson.getLock(anyString())).thenReturn(redissonLock);
        quota = new R2StorageQuota(properties, accountUsage, multipart, storage, redis, redisson, mapper);
    }

    @Test
    void concurrentUploadsToDifferentBucketsShareTheRemainingSpace() throws Exception {
        when(accountUsage.read(Set.of())).thenReturn(new R2AccountUsage.Usage(9_000_000_000L, Map.of()));
        quota.reconcile();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var outcomes = executor.invokeAll(List.<Callable<Boolean>>of(
                    () -> tryReserve("sqb-public"),
                    () -> tryReserve("sqb-secret")));
            assertTrue(outcomes.get(0).get() ^ outcomes.get(1).get());
        }
        assertEquals(9_300_000_000L, state().projectedBytes());
    }

    @Test
    void rejectsWritesBeforeTheFirstSnapshotAndWhenTheAccountIsFull() {
        assertEquals(ResponseCode.STORAGE_QUOTA_UNAVAILABLE, assertThrows(AppException.class,
                () -> quota.putObject(oneByteObject())).getResponseCode());
        when(accountUsage.read(Set.of())).thenReturn(new R2AccountUsage.Usage(9_500_000_000L, Map.of()));
        quota.reconcile();
        assertEquals(ResponseCode.STORAGE_QUOTA_EXCEEDED, assertThrows(AppException.class,
                () -> quota.putObject(oneByteObject())).getResponseCode());
        verifyNoInteractions(storage);
    }

    @Test
    void releasingAnUnissuedBatchReturnsItsCapacityWithoutAnInventoryRefresh() {
        when(accountUsage.read(Set.of())).thenReturn(new R2AccountUsage.Usage(9_000_000_000L, Map.of()));
        quota.reconcile();
        var uploads = List.of(new SizedUpload("a", 200), new SizedUpload("b", 300));
        quota.reservePresigned("sqb-public", uploads, Instant.now().plusSeconds(60));
        assertEquals(9_000_000_500L, state().projectedBytes());

        quota.releaseUnissuedPresigned("sqb-public", uploads);
        assertEquals(9_000_000_000L, state().projectedBytes());
    }

    @Test
    void inventoryReleasesSettledWritesAndKeepsHoldsCreatedDuringTheScan() {
        var settled = new Hold("sqb-secret", "exports/a", 50, 10, Kind.SETTLED, null, 0);
        var signed = new Hold("sqb-public", "images/b", 30, 100, Kind.PRESIGNED, null, 0);
        var newWrite = new Hold("sqb-public", "images/c", 20, 100, Kind.DIRECT, null, 0);
        var before = new State(100, 0, Map.of("old", settled, "signed", signed));
        var current = new State(100, 0, Map.of("old", settled, "signed", signed, "new", newWrite));

        var updated = R2StorageQuota.applyInventory(before, current, 20, Set.of(),
                new R2AccountUsage.Usage(180, Map.of(signed.ref(), 30L)));

        assertEquals(Set.of("signed", "new"), updated.holds().keySet());
        assertEquals(200, updated.projectedBytes());

        var afterDeletion = R2StorageQuota.applyInventory(updated, updated, 30, Set.of(),
                new R2AccountUsage.Usage(150, Map.of()));
        assertEquals(200, afterDeletion.projectedBytes());
    }

    @Test
    void keepsExpiredMultipartReservedUntilAbortSucceeds() {
        when(accountUsage.read(Set.of())).thenReturn(new R2AccountUsage.Usage(100, Map.of()));
        var abort = new AtomicReference<>(CompletableFuture.<Void>failedFuture(new IllegalStateException("temporary")));
        when(multipart.abortMultipartUpload(any(AbortMultipartUploadArgs.class)))
                .thenAnswer(call -> abort.get());
        quota.reconcile();
        quota.reserveMultipart("sqb-public", "video", 50, Instant.now().minusSeconds(1).toEpochMilli(), "upload");

        quota.reconcile();
        assertEquals(150, state().projectedBytes());

        abort.set(CompletableFuture.completedFuture(null));
        quota.reconcile();
        assertEquals(100, state().projectedBytes());
        assertTrue(state().holds().isEmpty());
    }

    private boolean tryReserve(String bucket) {
        try {
            quota.reservePresigned(bucket, List.of(new SizedUpload("fixture", 300_000_000L)), Instant.now().plusSeconds(60));
            return true;
        } catch (AppException failure) {
            assertEquals(ResponseCode.STORAGE_QUOTA_EXCEEDED, failure.getResponseCode());
            return false;
        }
    }

    private State state() {
        return mapper.readValue(data.get(), State.class);
    }

    private PutObjectArgs oneByteObject() {
        return PutObjectArgs.builder().bucket("sqb-public").object("fixture")
                .stream(new ByteArrayInputStream(new byte[1]), 1L, -1L).build();
    }
}
