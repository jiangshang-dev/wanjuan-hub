package com.heima.modules.auth.service;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.mail.MailService;
import com.heima.infrastructure.redis.RedisRateLimiter;
import com.heima.infrastructure.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {
    private final StringRedisTemplate redis;
    private final RedisRateLimiter limiter;
    private final MailService mailService;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    public void sendEmail(String rawScene, String email, String ip) {
        String scene = scene(rawScene,Set.of("REGISTER","LOGIN","RESET","BIND"));
        limiter.check("email-send:ip:"+ip,10,Duration.ofMinutes(10));
        limiter.check("email-send:"+scene+":"+email,5,Duration.ofHours(1));
        Boolean allowed = redis.opsForValue().setIfAbsent("wanjuan:code-frequency:EMAIL:"+scene+":"+email,"1",Duration.ofMinutes(1));
        if (!Boolean.TRUE.equals(allowed)) throw new ServiceException(429,"验证码发送过于频繁，请稍后再试");
        String code = code();
        String key = key("EMAIL",scene,email);
        redis.opsForValue().set(key,code,Duration.ofMinutes(10));
        try { mailService.sendCode(email,scene,code); }
        catch (RuntimeException e) { redis.delete(key); redis.delete("wanjuan:code-frequency:EMAIL:"+scene+":"+email); throw e; }
    }

    public void sendSms(String rawScene, String phone, String ip) {
        String scene = scene(rawScene,Set.of("LOGIN","BIND"));
        limiter.check("sms-send:ip:"+ip,10,Duration.ofMinutes(10));
        limiter.check("sms-send:"+scene+":"+phone,5,Duration.ofHours(1));
        Boolean allowed = redis.opsForValue().setIfAbsent("wanjuan:code-frequency:SMS:"+scene+":"+phone,"1",Duration.ofMinutes(1));
        if (!Boolean.TRUE.equals(allowed)) throw new ServiceException(429,"验证码发送过于频繁，请稍后再试");
        String code = code();
        String key = key("SMS",scene,phone);
        redis.opsForValue().set(key,code,Duration.ofMinutes(10));
        try { smsSender.sendCode(phone,scene,code); }
        catch (RuntimeException e) { redis.delete(key); redis.delete("wanjuan:code-frequency:SMS:"+scene+":"+phone); throw e; }
    }

    public void consumeEmail(String scene, String email, String code) { consume("EMAIL",scene,email,code); }
    public void consumeSms(String scene, String phone, String code) { consume("SMS",scene,phone,code); }

    private void consume(String channel, String scene, String target, String code) {
        String failureKey = "code:"+channel+":"+scene+":"+target;
        limiter.checkFailures(failureKey,5);
        String key = key(channel,scene,target);
        String expected = redis.opsForValue().getAndDelete(key);
        if (expected == null || code == null || !expected.equals(code.trim())) {
            limiter.failure(failureKey,5,Duration.ofMinutes(15));
            throw new ServiceException(400,"验证码错误或已失效");
        }
        limiter.clearFailures(failureKey);
    }

    private String key(String channel,String scene,String target) { return "wanjuan:verify-code:"+channel+":"+scene+":"+target.toLowerCase(Locale.ROOT); }
    private String code() { return String.format("%06d",random.nextInt(1_000_000)); }
    private String scene(String value,Set<String> allowed) {
        String scene = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(scene)) throw new ServiceException(400,"不支持的验证码场景");
        return scene;
    }
}
