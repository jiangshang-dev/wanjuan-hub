package com.heima.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "wanjuan.rustfs")
public class RustFsProperties {
    private String endpoint;
    private String region = "us-east-1";
    private String publicBucket;
    private String privateBucket;
    private String auditBucket;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true;
    private Duration presignExpire = Duration.ofMinutes(5);
    private long maxImageBytes = 10 * 1024 * 1024;

    public boolean isConfigured() {
        return nonBlank(endpoint) && nonBlank(region) && nonBlank(publicBucket)
                && nonBlank(privateBucket) && nonBlank(auditBucket)
                && nonBlank(accessKey) && nonBlank(secretKey);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
