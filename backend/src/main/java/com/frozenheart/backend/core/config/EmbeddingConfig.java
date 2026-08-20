package com.frozenheart.backend.core.config;

import com.frozenheart.backend.core.config.property.EmbeddingServiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(EmbeddingServiceProperties.class)
public class EmbeddingConfig {

    @Bean
    public RestClient embeddingRestClient(EmbeddingServiceProperties properties) {
        String baseUrl = properties.getUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8000"; // Default fallback
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
