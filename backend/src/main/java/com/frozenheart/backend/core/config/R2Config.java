package com.frozenheart.backend.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.modules.media.service.impl.R2MultipartClient;

import io.minio.MinioAsyncClient;
import io.minio.MinioClient;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {

    @Bean
    public MinioClient minioClient(R2Properties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .region(properties.getRegion())
                .build();
    }

    @Bean
    public R2MultipartClient r2MultipartClient(R2Properties properties) {
        return new R2MultipartClient(MinioAsyncClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .region(properties.getRegion())
                .build());
    }

}
