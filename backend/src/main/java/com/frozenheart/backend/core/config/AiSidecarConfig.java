package com.frozenheart.backend.core.config;

import com.frozenheart.backend.core.config.property.AiSidecarProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiSidecarProperties.class)
public class AiSidecarConfig {

    @Bean
    public RestClient aiSidecarRestClient(AiSidecarProperties properties) {
        String baseUrl = properties.getUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8000";
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Math.max(1000, properties.getTimeout());
        requestFactory.setConnectTimeout(Duration.ofMillis(10000));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
