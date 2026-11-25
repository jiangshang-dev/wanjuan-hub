package com.heima.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wanjuan")
public class JwtProperties {
    private String jwtSecret;
    private long jwtAccessExpireMinutes = 30;
    private long jwtRefreshExpireDays = 7;
}
