package com.heima.modules.auth.service;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.redis.RedisRateLimiter;
import com.heima.modules.auth.dto.AuthDtos.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaService {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;
    private final RedisRateLimiter limiter;

    public CaptchaResponse create(String ip) {
        limiter.check("captcha:" + ip, 30, Duration.ofMinutes(1));
        String id = UUID.randomUUID().toString();
        StringBuilder code = new StringBuilder();
        for (int i=0;i<4;i++) code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        BufferedImage image = new BufferedImage(140, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(248,250,252)); g.fillRect(0,0,140,48);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i=0;i<7;i++) {
            g.setColor(new Color(120+random.nextInt(100),120+random.nextInt(100),120+random.nextInt(100)));
            g.drawLine(random.nextInt(140),random.nextInt(48),random.nextInt(140),random.nextInt(48));
        }
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,28));
        for (int i=0;i<code.length();i++) {
            g.setColor(new Color(30+random.nextInt(100),60+random.nextInt(100),100+random.nextInt(100)));
            g.drawString(String.valueOf(code.charAt(i)),15+i*29,32+random.nextInt(7));
        }
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image,"png",out);
            redis.opsForValue().set("wanjuan:captcha:"+id,code.toString(),Duration.ofMinutes(5));
            return new CaptchaResponse(id,"data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray()));
        } catch (Exception e) { throw new ServiceException(500,"验证码生成失败"); }
    }

    public void verify(String id, String code) {
        limiter.checkFailures("captcha:"+id,5);
        String key = "wanjuan:captcha:"+id;
        String expected = redis.opsForValue().getAndDelete(key);
        if (expected == null || code == null || !expected.equals(code.trim().toUpperCase(Locale.ROOT))) {
            limiter.failure("captcha:"+id,5,Duration.ofMinutes(10));
            throw new ServiceException(400,"图形验证码错误或已失效");
        }
        limiter.clearFailures("captcha:"+id);
    }
}
