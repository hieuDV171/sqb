package com.frozenheart.backend.modules.media.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.media.MediaType;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.dto.LectureVideoUpload;
import com.frozenheart.backend.modules.media.service.impl.R2MultipartClient;
import io.minio.AbortMultipartUploadArgs;
import io.minio.CompleteMultipartUploadArgs;
import io.minio.CreateMultipartUploadArgs;
import io.minio.Http;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static com.frozenheart.backend.modules.media.service.impl.MediaServiceImpl.REDIS_PENDING_MEDIA_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureVideoUploadService {
    private static final String OBJECT_PREFIX = "lecture-videos/";
    private static final String SESSION_PREFIX = "sqb:lecture-upload:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    static final long PART_SIZE = 16 * 1024 * 1024L;
    private static final int MAX_PARTS = 10_000;
    private static final int PART_URL_TTL_SECONDS = 15 * 60;
    private static final Map<String, String> VIDEO_EXTENSIONS = Map.of("video/mp4", ".mp4", "video/webm", ".webm");

    private final R2Properties properties;
    private final R2MultipartClient r2;
    private final MediaService mediaService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final JsonMapper jsonMapper;

    public LectureVideoUpload.Status start(UUID id, LectureVideoUpload.StartRequest request) {
        long ownerId = lecturerId();
        String extension = VIDEO_EXTENSIONS.get(request.contentType());
        if (extension == null) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Vui lòng chọn video MP4 hoặc WebM.");
        }
        long maximum = Math.min(properties.getLectureVideoMaxFileSize().toBytes(), PART_SIZE * MAX_PARTS);
        if (request.fileSize() <= 0 || request.fileSize() > maximum) {
            throw new AppException(ResponseCode.FILE_SIZE_EXCEEDED,
                    "Video phải có dung lượng lớn hơn 0 và tối đa %.2f GiB."
                            .formatted(maximum / (1024.0 * 1024 * 1024)));
        }

        return withSessionLock(id, () -> {
            Session existing = find(id);
            if (existing != null) {
                checkOwner(existing, ownerId);
                if (existing.state() == State.CANCELLED) throw expired();
                if (!existing.matches(request)) {
                    throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Phiên tải thuộc về tệp khác.");
                }
                return getStatus(id, existing);
            }

            String key = OBJECT_PREFIX + id + "/" + UUID.randomUUID() + extension;
            String uploadId = r2.createMultipartUpload(CreateMultipartUploadArgs.builder()
                    .bucket(properties.getBucketName()).object(key)
                    .headers(new Http.Headers("Content-Type", request.contentType()))
                    .build()).join().result().uploadId();
            var session = new Session(ownerId, key, uploadId, request.fileName(), request.contentType(),
                    request.fileSize(), Instant.now().plus(SESSION_TTL).toEpochMilli(), State.UPLOADING);
            try {
                save(id, session);
            } catch (RuntimeException failure) {
                try {
                    abort(session);
                } catch (RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
            return status(id, List.of(), null);
        });
    }

    public LectureVideoUpload.Status status(UUID id) {
        long ownerId = lecturerId();
        return withSessionLock(id, () -> getStatus(id, requireSession(id, ownerId)));
    }

    public LectureVideoUpload.PartUrl partUrl(UUID id, int number) {
        long ownerId = lecturerId();
        return withSessionLock(id, () -> {
            Session session = requireSession(id, ownerId);
            requireUploading(session);
            if (number < 1 || number > partCount(session.size())) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Số thứ tự phần tải không hợp lệ.");
            }
            try {
                String url = r2.presignPart(properties.getBucketName(), session.key(), session.uploadId(), number,
                        partSize(session.size(), number), PART_URL_TTL_SECONDS);
                return new LectureVideoUpload.PartUrl(url);
            } catch (MinioException exception) {
                throw storageFailure(exception);
            }
        });
    }

    public MediaItem complete(UUID id) {
        long ownerId = lecturerId();
        return withSessionLock(id, () -> {
            Session session = requireSession(id, ownerId);
            if (session.state() == State.READY) {
                return media(session);
            }
            requireUploading(session);

            List<Part> parts = uploadedParts(session);
            if (parts == null) {
                return verifyCompleted(id, session);
            }
            List<Part> valid = validParts(parts, session.size());
            if (valid.size() != partCount(session.size())) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Video còn phần chưa tải xong. Hãy thử tải tiếp.");
            }

            // Register before completion so the cleanup job can find objects even if the response is lost.
            trackPending(session.key());
            Part[] manifest = valid.stream()
                    .map(part -> new Part(part.partNumber(), "\"" + part.etag() + "\""))
                    .toArray(Part[]::new);
            r2.completeMultipartUpload(CompleteMultipartUploadArgs.builder()
                    .bucket(properties.getBucketName()).object(session.key()).uploadId(session.uploadId())
                    .parts(manifest).build()).join();
            return verifyCompleted(id, session);
        });
    }

    public void cancel(UUID id) {
        long ownerId = lecturerId();
        withSessionLock(id, () -> {
            Session session = find(id);
            if (session != null) {
                checkOwner(session, ownerId);
                if (session.state() == State.ATTACHED || session.state() == State.CANCELLED) return null;
                if (session.state() == State.UPLOADING) abort(session);
                save(id, session.withState(State.CANCELLED));
            } else {
                // The browser may cancel before its start request reaches the server.
                var cancelled = new Session(ownerId, null, null, null, null, 0,
                        Instant.now().plus(SESSION_TTL).toEpochMilli(), State.CANCELLED);
                save(id, cancelled);
            }
            return null;
        });
    }

    public List<MediaItem> preparePostMedia(List<MediaItem> requested, List<MediaItem> existing, boolean lecturePost,
                                           long ownerId) {
        if (requested == null || requested.isEmpty()) {
            return requested;
        }
        var prepared = new ArrayList<MediaItem>(requested.size());
        var previousMedia = new HashMap<String, MediaItem>();
        if (existing != null) {
            for (MediaItem item : existing) {
                if (item == null) continue;
                String key = lectureKey(item);
                if (key != null) previousMedia.put(key, item);
            }
        }
        var attachedKeys = new HashSet<String>();
        for (MediaItem item : requested) {
            String key = lectureKey(item);
            if (key == null) {
                prepared.add(item);
                continue;
            }
            if (!lecturePost || !attachedKeys.add(key)) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Tệp tải bài giảng chỉ được gắn một lần trong bài giảng.");
            }
            MediaItem previous = previousMedia.get(key);
            prepared.add(previous != null ? previous : reserveForPost(key, ownerId));
        }
        return prepared;
    }

    public boolean isLectureUploadKey(String key) {
        return key != null && key.startsWith(OBJECT_PREFIX);
    }

    private MediaItem reserveForPost(String key, long ownerId) {
        UUID id;
        try {
            String[] segments = key.split("/", 3);
            if (segments.length != 3) throw expired();
            id = UUID.fromString(segments[1]);
        } catch (IllegalArgumentException exception) {
            throw expired();
        }
        return withSessionLock(id, () -> {
            Session session = requireSession(id, ownerId);
            if (session.state() != State.READY || !key.equals(session.key())) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Video chưa tải xong hoặc đã được gắn vào bài giảng.");
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        withSessionLock(id, () -> {
                            trackPending(key);
                            if (session.expiresAt() > System.currentTimeMillis()) {
                                save(id, session);
                            }
                            return null;
                        });
                    }
                }
            });
            save(id, session.withState(State.ATTACHED));
            mediaService.confirmMediaPermanent(List.of(key));
            return media(session);
        });
    }

    private String lectureKey(MediaItem item) {
        if (item == null) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Tệp đính kèm không hợp lệ.");
        }
        String urlKey = mediaService.extractObjectKey(item.url());
        if (isLectureUploadKey(urlKey)) return urlKey;
        String key = mediaService.extractObjectKey(item.key());
        return isLectureUploadKey(key) ? key : null;
    }

    private LectureVideoUpload.Status getStatus(UUID id, Session session) {
        if (session.state() == State.READY) {
            return status(id, List.of(), media(session));
        }
        requireUploading(session);
        List<Part> parts = uploadedParts(session);
        if (parts == null) {
            return status(id, List.of(), verifyCompleted(id, session));
        }
        return status(id, validParts(parts, session.size()).stream().map(Part::partNumber).toList(), null);
    }

    private MediaItem verifyCompleted(UUID id, Session session) {
        try {
            var object = r2.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucketName()).object(session.key()).build()).join();
            if (object.size() != session.size() || !session.contentType().equals(object.contentType())) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Kích thước hoặc định dạng video không khớp.");
            }
        } catch (CompletionException exception) {
            if (hasStorageCode(exception, "NoSuchKey", "NotFound", "NoSuchObject")) {
                throw expired();
            }
            throw exception;
        }
        trackPending(session.key());
        save(id, session.ready());
        return media(session);
    }

    private List<Part> uploadedParts(Session session) {
        var parts = new ArrayList<Part>();
        Integer marker = null;
        try {
            do {
                var page = r2.listParts(properties.getBucketName(), session.key(), session.uploadId(), marker);
                parts.addAll(page.parts());
                marker = page.isTruncated() ? page.nextPartNumberMarker() : null;
            } while (marker != null);
        } catch (CompletionException exception) {
            if (hasStorageCode(exception, "NoSuchUpload")) {
                return null;
            }
            throw exception;
        }
        return parts;
    }

    static List<Part> validParts(List<Part> parts, long size) {
        return parts.stream()
                .filter(part -> part.partNumber() >= 1 && part.partNumber() <= partCount(size)
                        && part.partSize() == partSize(size, part.partNumber()))
                .sorted(Comparator.comparingInt(Part::partNumber))
                .toList();
    }

    private static long partCount(long size) {
        return Math.ceilDiv(size, PART_SIZE);
    }

    private static long partSize(long size, int number) {
        return Math.min(PART_SIZE, size - (number - 1L) * PART_SIZE);
    }

    private void abort(Session session) {
        try {
            r2.abortMultipartUpload(AbortMultipartUploadArgs.builder().bucket(properties.getBucketName())
                    .object(session.key()).uploadId(session.uploadId()).build()).join();
        } catch (CompletionException exception) {
            if (!hasStorageCode(exception, "NoSuchUpload")) {
                throw exception;
            }
        }
    }

    private void trackPending(String key) {
        redisTemplate.opsForZSet().add(REDIS_PENDING_MEDIA_KEY, key, Instant.now().toEpochMilli());
    }

    private Session find(UUID id) {
        String value = stringRedisTemplate.opsForValue().get(SESSION_PREFIX + id);
        return value == null ? null : jsonMapper.readValue(value, Session.class);
    }

    private void save(UUID id, Session session) {
        long remaining = session.expiresAt() - System.currentTimeMillis();
        if (remaining <= 0) {
            throw expired();
        }
        stringRedisTemplate.opsForValue().set(SESSION_PREFIX + id, jsonMapper.writeValueAsString(session),
                Duration.ofMillis(remaining));
    }

    private Session requireSession(UUID id, long ownerId) {
        Session session = find(id);
        if (session == null) {
            throw expired();
        }
        checkOwner(session, ownerId);
        return session;
    }

    private void checkOwner(Session session, long ownerId) {
        if (session.ownerId() != ownerId) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Phiên tải thuộc về tài khoản khác.");
        }
    }

    private void requireUploading(Session session) {
        if (session.state() == State.CANCELLED) throw expired();
        if (session.state() != State.UPLOADING) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Phiên tải video đã kết thúc.");
        }
    }

    private long lecturerId() {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        if (!UserRole.LECTURER.name().equals(payload.getRole()) && !UserRole.ADMIN.name().equals(payload.getRole())) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Chỉ Giảng viên hoặc Quản trị viên được tải video bài giảng.");
        }
        return payload.getUserId();
    }

    private <T> T withSessionLock(UUID id, Supplier<T> operation) {
        var lock = redissonClient.getLock(SESSION_PREFIX + id + ":lock");
        lock.lock();
        try {
            return operation.get();
        } catch (CompletionException exception) {
            throw storageFailure(exception.getCause());
        } finally {
            lock.unlock();
        }
    }

    private AppException storageFailure(Throwable cause) {
        log.warn("R2 lecture video upload operation failed", cause);
        return new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Kho lưu trữ đang gặp lỗi. Vui lòng thử lại.");
    }

    private boolean hasStorageCode(CompletionException exception, String... codes) {
        return exception.getCause() instanceof ErrorResponseException error && error.errorResponse() != null
                && List.of(codes).contains(error.errorResponse().code());
    }

    private static AppException expired() {
        return new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Phiên tải video đã hết hạn. Hãy chọn lại tệp.");
    }

    private LectureVideoUpload.Status status(UUID id, List<Integer> uploaded, MediaItem media) {
        return new LectureVideoUpload.Status(id, PART_SIZE, uploaded, media);
    }

    private MediaItem media(Session session) {
        return new MediaItem(session.key(), mediaService.buildPublicUrl(session.key()), MediaType.VIDEO,
                null, null, session.size(), null, session.fileName(), null);
    }

    private enum State { UPLOADING, READY, ATTACHED, CANCELLED }

    private record Session(long ownerId, String key, String uploadId, String fileName, String contentType,
                           long size, long expiresAt, State state) {
        boolean matches(LectureVideoUpload.StartRequest request) {
            return size == request.fileSize() && fileName.equals(request.fileName()) && contentType.equals(request.contentType());
        }

        Session ready() {
            return new Session(ownerId, key, uploadId, fileName, contentType, size,
                    Instant.now().plus(SESSION_TTL).toEpochMilli(), State.READY);
        }

        Session withState(State next) {
            return new Session(ownerId, key, uploadId, fileName, contentType, size, expiresAt, next);
        }
    }
}
