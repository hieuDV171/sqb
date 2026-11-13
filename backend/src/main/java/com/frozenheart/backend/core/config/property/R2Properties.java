package com.frozenheart.backend.core.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "r2")
public class R2Properties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region = "auto";
    private String publicUrl;
    private DataSize lectureVideoMaxFileSize = DataSize.ofGigabytes(2);
}
