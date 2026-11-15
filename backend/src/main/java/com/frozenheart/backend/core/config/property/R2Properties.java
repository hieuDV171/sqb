package com.frozenheart.backend.core.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

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
    private String reportEvidenceBucket;
    private int reportEvidenceRetentionDays = 90;
    private DataSize lectureVideoMaxFileSize = DataSize.ofGigabytes(2);
    private boolean storageQuotaEnabled;
    private long storageWarningBytes = 9_000_000_000L;
    private long storageLimitBytes = 9_500_000_000L;
    private String accountId;
    private String metricsApiToken;
    private List<String> quotaJurisdictions = List.of("default");
}
