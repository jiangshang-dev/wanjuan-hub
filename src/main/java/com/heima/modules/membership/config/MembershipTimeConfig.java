package com.heima.modules.membership.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class MembershipTimeConfig {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    public Clock membershipClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
