package com.heima.security.token;

import com.heima.exception.ServiceException;
import com.heima.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final String ACCESS="ACCESS", REFRESH="REFRESH";
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties=properties;
        byte[] secret;
        if (properties.getJwtSecret()==null || properties.getJwtSecret().length()<32) {
            secret=new byte[48]; new SecureRandom().nextBytes(secret);
        } else secret=properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        this.key=new SecretKeySpec(secret,"HmacSHA256");
    }

    public String access(String userType,Long userId,int version) {
        Instant now=Instant.now();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(String.valueOf(userId))
                .claim("typ",userType).claim("tokenType",ACCESS).claim("ver",version)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.getJwtAccessExpireMinutes()*60)))
                .signWith(key).compact();
    }

    public GeneratedRefresh refresh(String userType,Long userId,int version) {
        Instant now=Instant.now(); Instant expiry=now.plusSeconds(properties.getJwtRefreshExpireDays()*86400);
        String id=UUID.randomUUID().toString();
        String token=Jwts.builder().id(id).subject(String.valueOf(userId)).claim("typ",userType)
                .claim("tokenType",REFRESH).claim("ver",version).issuedAt(Date.from(now)).expiration(Date.from(expiry)).signWith(key).compact();
        return new GeneratedRefresh(token,id,LocalDateTime.ofInstant(expiry,ZoneId.systemDefault()));
    }

    public Claims parse(String token,String expectedType) {
        try {
            Claims claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get("tokenType",String.class))) throw new ServiceException(401,"令牌类型错误");
            return claims;
        } catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException(401,"令牌无效或已过期"); }
    }

    public long accessExpiresIn() { return properties.getJwtAccessExpireMinutes()*60; }
    public String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public record GeneratedRefresh(String token,String tokenId,LocalDateTime expiresAt) {}
}
