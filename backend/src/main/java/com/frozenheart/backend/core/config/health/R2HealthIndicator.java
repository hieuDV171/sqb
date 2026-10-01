package com.frozenheart.backend.core.config.health;

import com.frozenheart.backend.core.config.property.R2Properties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Health check cho Cloudflare R2 Object Storage (chuẩn S3 API).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class R2HealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final R2Properties r2Properties;

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(r2Properties.getBucketName())
                            .build()
            );

            if (exists) {
                return Health.up()
                        .withDetail("storage", "Cloudflare R2")
                        .withDetail("bucket", r2Properties.getBucketName())
                        .withDetail("endpoint", r2Properties.getEndpoint())
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Bucket not found: " + r2Properties.getBucketName())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Cloudflare R2 health check failed: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("storage", "Cloudflare R2")
                    .withDetail("endpoint", r2Properties.getEndpoint())
                    .build();
        }
    }
}
