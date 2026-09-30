package com.frozenheart.backend.core.config.health;

import com.frozenheart.backend.core.config.property.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Health check cho MinIO Object Storage.
 * TODO Cloud Migration:
 * Khi chuyển đổi từ MinIO nội bộ sang Cloudflare R2 hoặc AWS S3:
 * MinIO Client (hoặc AWS S3 SDK) đều hỗ trợ chuẩn S3 API (headBucket / bucketExists).
 * Ta chỉ cần cập nhật cấu hình endpoint, accessKey, secretKey và region tương ứng với R2/S3
 * mà không cần phải thay đổi logic nghiệp vụ của HealthIndicator này.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );

            if (exists) {
                return Health.up()
                        .withDetail("storage", "S3/MinIO")
                        .withDetail("bucket", minioProperties.getBucketName())
                        .withDetail("endpoint", minioProperties.getEndpoint())
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Bucket not found: " + minioProperties.getBucketName())
                        .build();
            }
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("storage", "S3/MinIO")
                    .withDetail("endpoint", minioProperties.getEndpoint())
                    .build();
        }
    }
}
