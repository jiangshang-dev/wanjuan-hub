package com.heima.infrastructure.redis;

import com.heima.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final StringRedisTemplate redis;

    public void check(String key, int limit, Duration window) {
        Long count = redis.opsForValue().increment("wanjuan:rate:" + key);
        if (count != null && count == 1L) redis.expire("wanjuan:rate:" + key, window);
        if (count != null && count > limit) throw new ServiceException(429, "操作过于频繁，请稍后重试");
    }

    public void failure(String key, int limit, Duration window) {
        Long count = redis.opsForValue().increment("wanjuan:fail:" + key);
        if (count != null && count == 1L) redis.expire("wanjuan:fail:" + key, window);
        if (count != null && count >= limit) throw new ServiceException(429, "失败次数过多，请稍后重试");
    }

    public void checkFailures(String key, int limit) {
        String value = redis.opsForValue().get("wanjuan:fail:" + key);
        if (value != null && Integer.parseInt(value) >= limit) throw new ServiceException(429, "失败次数过多，请稍后重试");
    }

    public void clearFailures(String key) { redis.delete("wanjuan:fail:" + key); }
}
