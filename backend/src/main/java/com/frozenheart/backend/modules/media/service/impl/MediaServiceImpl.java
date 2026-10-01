package com.frozenheart.backend.modules.media.service.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.imageio.ImageIO;

import java.net.URI;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.StatObjectArgs;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.media.MediaPurpose;
import com.frozenheart.backend.core.entity.media.MediaType;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.dto.MediaUploadResponse;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest;
import com.frozenheart.backend.modules.media.dto.PresignMediaResponse;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest.PresignFileItem;
import com.frozenheart.backend.modules.media.dto.PresignMediaResponse.PresignedUrlItem;
import com.frozenheart.backend.modules.media.service.MediaService;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MinioClient minioClient;
    private final R2Properties r2Properties;
    private final RedisTemplate<String, String> redisTemplate;

    public static final String REDIS_PENDING_MEDIA_KEY = "sqb:media:pending";

    @Override
    public MediaUploadResponse uploadMedia(MultipartFile file, MediaPurpose purpose) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "File upload không được để trống");
        }

        validateFileSize(purpose, file.getSize(), file.getOriginalFilename());

        try {
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String objectKey = generateObjectKey(purpose, originalFilename);

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(r2Properties.getBucketName())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1L)
                                .contentType(contentType)
                                .build());
            }

            // Ghi nhận objectKey vào hàng đợi pending trên Redis với timestamp UTC
            redisTemplate.opsForZSet().add(REDIS_PENDING_MEDIA_KEY, objectKey, Instant.now().toEpochMilli());

            Integer width = null;
            Integer height = null;
            MediaType mediaType = determineMediaType(contentType);

            if (mediaType == MediaType.IMAGE) {
                try (InputStream imageStream = file.getInputStream()) {
                    BufferedImage bufferedImage = ImageIO.read(imageStream);
                    if (bufferedImage != null) {
                        width = bufferedImage.getWidth();
                        height = bufferedImage.getHeight();
                    }
                } catch (Exception e) {
                    log.warn("[MediaServiceImpl] Không thể đọc kích thước ảnh: {}", e.getMessage());
                }
            }

            String publicUrl = buildPublicUrl(objectKey);

            return MediaUploadResponse.builder()
                    .objectKey(objectKey)
                    .url(publicUrl)
                    .fileSize(file.getSize())
                    .contentType(contentType)
                    .mediaType(mediaType)
                    .width(width)
                    .height(height)
                    .build();

        } catch (Exception e) {
            log.error("[MediaServiceImpl] Lỗi khi upload file lên R2: ", e);
            throw new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Upload file thất bại: " + e.getMessage());
        }

    }

    @Override
    public void confirmMediaPermanent(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) return;

        for (String rawKey : objectKeys) {
            String objectKey = extractObjectKey(rawKey);
            if (objectKey == null) continue;
            redisTemplate.opsForZSet().remove(REDIS_PENDING_MEDIA_KEY, objectKey);
            log.info("[R2] Đã xác nhận file permanent và xóa khỏi pending queue: {}", objectKey);
        }
    }

    @Override
    public void deleteMedia(List<String> urlsOrKeys) {
        if (urlsOrKeys == null || urlsOrKeys.isEmpty()) return;

        List<DeleteRequest.Object> objectsToDelete = urlsOrKeys.stream()
                .map(this::extractObjectKey)
                .filter(Objects::nonNull)
                .distinct()
                .map(DeleteRequest.Object::new)
                .toList();

        if (objectsToDelete.isEmpty()) return;

        try {
            Iterable<Result<DeleteResult.Error>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(r2Properties.getBucketName())
                            .objects(objectsToDelete)
                            .build()
            );

            // Duyệt qua kết quả batch delete để bắt lỗi nếu có
            for (Result<DeleteResult.Error> result : results) {
                DeleteResult.Error error = result.get();
                log.error("[R2] Lỗi khi xóa object {}: {}", error.objectName(), error.message());
            }
            log.info("[R2] Đã hoàn tất yêu cầu xóa {} objects khỏi bucket {}", objectsToDelete.size(), r2Properties.getBucketName());
        } catch (Exception e) {
            log.error("[R2] Lỗi khi thực hiện xóa batch objects: ", e);
        }
    }

    @Override
    public String extractObjectKey(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return null;
        }
        String clean = urlOrKey.trim();

        // Loại bỏ query parameter nếu có (ví dụ presigned URL: ?token=...)
        int queryIdx = clean.indexOf('?');
        if (queryIdx != -1) {
            clean = clean.substring(0, queryIdx);
        }

        // Trường hợp URL tuyệt đối chứa bucket name (/sqb/...)
        String bucketPattern = "/" + r2Properties.getBucketName() + "/";
        int bucketIdx = clean.indexOf(bucketPattern);
        if (bucketIdx != -1) {
            clean = clean.substring(bucketIdx + bucketPattern.length());
        } else if (clean.startsWith("http://") || clean.startsWith("https://")) {
            try {
                URI uri = URI.create(clean);
                String path = uri.getPath();
                if (path != null) {
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    if (path.startsWith(r2Properties.getBucketName() + "/")) {
                        path = path.substring((r2Properties.getBucketName() + "/").length());
                    }
                    clean = path;
                }
            } catch (Exception e) {
                log.warn("[MediaServiceImpl] Không thể parse URI từ: {}", urlOrKey);
            }
        }

        // Xóa dấu slash ở đầu nếu còn sót
        while (clean != null && clean.startsWith("/")) {
            clean = clean.substring(1);
        }

        return (clean != null && !clean.isBlank()) ? clean : null;
    }

    private boolean isObjectExists(String urlOrKey) {
        String objectKey = extractObjectKey(urlOrKey);
        if (objectKey == null) return false;
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(r2Properties.getBucketName())
                            .object(objectKey)
                            .build()
            );
            return stat != null;
        } catch (io.minio.errors.ErrorResponseException e) {
            if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) {
                log.warn("[R2] Object không tồn tại: {}", objectKey);
                return false;
            }
            log.warn("[R2] Lỗi khi statObject {}: {}", objectKey, e.getMessage());
            return true;
        } catch (Exception e) {
            log.warn("[R2] Lỗi kiểm tra tồn tại file {}: {}", objectKey, e.getMessage());
            return true;
        }
    }

    @Override
    public List<String> findMissingObjects(List<String> urlsOrKeys) {
        if (urlsOrKeys == null || urlsOrKeys.isEmpty()) {
            return List.of();
        }

        // Tận dụng xử lý song song (parallelStream) để kiểm tra toàn bộ ảnh đồng thời
        return urlsOrKeys.parallelStream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(u -> !u.isBlank())
                .filter(u -> !u.startsWith("data:") && !u.startsWith("blob:"))
                .distinct()
                .filter(u -> !isObjectExists(u))
                .toList();
    }

    private String buildPublicUrl(String objectKey) {
        String publicUrl = r2Properties.getPublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            String clean = publicUrl.trim();
            if (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            return String.format("%s/%s", clean, objectKey);
        }
        String endpoint = r2Properties.getEndpoint();
        if (endpoint != null && endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return String.format("%s/%s/%s", endpoint, r2Properties.getBucketName(), objectKey);
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType == null)
            return MediaType.DOCUMENT;
        if (contentType.startsWith("image/"))
            return MediaType.IMAGE;
        if (contentType.startsWith("video/"))
            return MediaType.VIDEO;
        return MediaType.DOCUMENT;
    }

    // Sinh đường dẫn lưu trữ ObjectKey trên R2 theo cấu trúc: {folder}/{yyyy/MM}/{uuid}{extension}
    private String generateObjectKey(MediaPurpose purpose, String originalFilename) {
        String folder = (purpose != null) ? purpose.getFolderName() : "others";
        String datePath = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = getFileExtension(originalFilename);

        return String.format("%s/%s/%s%s", folder, datePath, UUID.randomUUID(), extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    @Override
    public PresignMediaResponse generatePresignedUrls(PresignMediaRequest request) {

        List<PresignedUrlItem> resultItems = new ArrayList<>();
        long currentTimestamp = Instant.now().toEpochMilli();

        for (PresignFileItem item : request.files()) {
            validateFileSize(item.purpose(), item.fileSize(), item.fileName());

            try {
                String objectKey = generateObjectKey(item.purpose(), item.fileName());

                String uploadUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(r2Properties.getBucketName())
                                .object(objectKey)
                                .expiry(Time.DEFAULT_EXPIRATION_SECONDS)
                                .build());

                // Ghi nhận objectKey vào hàng đợi pending trên Redis với timestamp UTC
                redisTemplate.opsForZSet().add(REDIS_PENDING_MEDIA_KEY, objectKey, currentTimestamp);

                String publicUrl = buildPublicUrl(objectKey);

                resultItems.add(PresignedUrlItem.builder()
                        .objectKey(objectKey)
                        .uploadUrl(uploadUrl)
                        .publicUrl(publicUrl)
                        .expiresInSeconds(Time.DEFAULT_EXPIRATION_SECONDS)
                        .build());
            } catch (Exception e) {
                log.error("[MediaServiceImpl] Lỗi khi sinh Presigned URL cho file {}: ", item.fileName(), e);
                throw new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Sinh presigned URL thất bại cho file: " + item.fileName());
            }
        }

        return PresignMediaResponse.builder()
                .presignedUrls(resultItems)
                .build();
    }

    private void validateFileSize(MediaPurpose purpose, long fileSize, String fileName) {
        long maxAllowedBytes = switch (purpose != null ? purpose : MediaPurpose.QUESTION) {
            case AVATAR -> 2 * 1024 * 1024L;     // 2 MB
            case COVER -> 4 * 1024 * 1024L;      // 4 MB
            case DOCUMENT -> 15 * 1024 * 1024L;  // 15 MB
            default -> 5 * 1024 * 1024L;         // 5 MB (QUESTION, POST, COMMENT, CHAT)
        };

        if (fileSize > maxAllowedBytes) {
            double maxMb = maxAllowedBytes / (1024.0 * 1024.0);
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    String.format("Tệp '%s' vượt quá dung lượng tối đa cho phép (%.1f MB) đối với mục đích %s",
                            fileName, maxMb, purpose));
        }
    }
}
