package com.frozenheart.backend.core.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "embedding.service")
public class EmbeddingServiceProperties {
    private String url;
    private long timeout;
}
