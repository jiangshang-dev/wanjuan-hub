package com.heima.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "wanjuan.security")
public class SecurityProperties {
    private List<String> allowedOrigins = List.of();
    private boolean trustForwardedHeaders;
}
