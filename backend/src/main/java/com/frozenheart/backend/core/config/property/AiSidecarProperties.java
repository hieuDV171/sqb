package com.frozenheart.backend.core.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.sidecar")
public class AiSidecarProperties {
    private String url = "http://localhost:8000";
    private long timeout = 45000;
}
