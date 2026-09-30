package com.frozenheart.backend.modules.media.service.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
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
import io.minio.SetObjectTagsArgs;
import io.minio.StatObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.frozenheart.backend.core.config.property.MinioProperties;
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
    private final MinioProperties minioProperties;

    @Override
    public MediaUploadResponse uploadMedia(MultipartFile file, MediaPurpose purpose) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "File upload không được để trống");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String objectKey = generateObjectKey(purpose, originalFilename);

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1L)
                                .contentType(contentType)
                                .tags(Map.of("status", "temp"))
                                .build());
            }

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
            log.error("[MediaServiceImpl] Lỗi khi upload file lên MinIO: ", e);
            throw new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Upload file thất bại: " + e.getMessage());
        }

    }

    @Override
    public void confirmMediaPermanent(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) return;

        Map<String, String> tagMap = Map.of("status", "permanent");

        for (String rawKey : objectKeys) {
            String objectKey = extractObjectKey(rawKey);
            if (objectKey == null) continue;
            try {
                minioClient.setObjectTags(
                    SetObjectTagsArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectKey)
                        .tags(tagMap)
                        .build()
                );
                log.info("[MinIO] Đã chuyển file {} sang status=permanent", objectKey);
            } catch (Exception e) {
                log.error("[MinIO] Lỗi khi cập nhật tag permanent cho file {}: ", objectKey, e);
            }
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
                            .bucket(minioProperties.getBucketName())
                            .objects(objectsToDelete)
                            .build()
            );

            // MinIO SDK trả về lazy iterable, duyệt qua để thực thi xóa và bắt lỗi
            for (Result<DeleteResult.Error> result : results) {
                DeleteResult.Error error = result.get();
                log.error("[MinIO] Lỗi khi xóa object {}: {}", error.objectName(), error.message());
            }
            log.info("[MinIO] Đã hoàn tất yêu cầu xóa {} objects khỏi bucket {}", objectsToDelete.size(), minioProperties.getBucketName());
        } catch (Exception e) {
            log.error("[MinIO] Lỗi khi thực hiện xóa batch objects: ", e);
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

        // Trường hợp URL tuyệt đối chứa bucket name (/sqb-bucket/...)
        String bucketPattern = "/" + minioProperties.getBucketName() + "/";
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
                    if (path.startsWith(minioProperties.getBucketName() + "/")) {
                        path = path.substring((minioProperties.getBucketName() + "/").length());
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
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .build()
            );
            return stat != null;
        } catch (io.minio.errors.ErrorResponseException e) {
            if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) {
                log.warn("[MinIO] Object không tồn tại hoặc đã bị ILM xóa: {}", objectKey);
                return false;
            }
            log.warn("[MinIO] Lỗi khi statObject {}: {}", objectKey, e.getMessage());
            return true; // Nếu lỗi khác (phân quyền/mạng tạm thời), không chặn oan
        } catch (Exception e) {
            log.warn("[MinIO] Lỗi kiểm tra tồn tại file {}: {}", objectKey, e.getMessage());
            return true;
        }
    }

    @Override
    public List<String> findMissingObjects(List<String> urlsOrKeys) {
        if (urlsOrKeys == null || urlsOrKeys.isEmpty()) {
            return List.of();
        }

        // Tận dụng xử lý song song (parallelStream) để kiểm tra toàn bộ ảnh đồng thời chỉ trong 10-15ms
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
        String endpoint = minioProperties.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return String.format("%s/%s/%s", endpoint, minioProperties.getBucketName(), objectKey);
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

    // Sinh đường dẫn lưu trữ ObjectKey trên MinIO theo cấu trúc: {folder}/{yyyy/MM}/{uuid}{extension}
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

        for (PresignFileItem item : request.files()) {

            try {
                String objectKey = generateObjectKey(item.purpose(), item.fileName());

                Map<String, String> extraQueryParams = new HashMap<>();
                extraQueryParams.put("x-amz-tagging", "status=temp");

                String uploadUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(minioProperties.getBucketName())
                                .object(objectKey)
                                .expiry(Time.DEFAULT_EXPIRATION_SECONDS)
                                .extraQueryParams(extraQueryParams)
                                .build());

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

}
