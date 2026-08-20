package com.frozenheart.backend.modules.media.service.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.frozenheart.backend.core.config.property.MinioProperties;
import com.frozenheart.backend.core.constant.ResponseCode;
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

    private static final int DEFAULT_PRESIGNED_EXPIRATION_SECONDS = 7200; // 2 hours

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
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
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

                String uploadUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(minioProperties.getBucketName())
                                .object(objectKey)
                                .expiry(DEFAULT_PRESIGNED_EXPIRATION_SECONDS)
                                .build());

                String publicUrl = buildPublicUrl(objectKey);

                resultItems.add(PresignedUrlItem.builder()
                        .objectKey(objectKey)
                        .uploadUrl(uploadUrl)
                        .publicUrl(publicUrl)
                        .expiresInSeconds(DEFAULT_PRESIGNED_EXPIRATION_SECONDS)
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
