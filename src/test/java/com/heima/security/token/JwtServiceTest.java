package com.heima.security.token;

import com.heima.exception.ServiceException;
import com.heima.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwt;

    JwtServiceTest() {
        JwtProperties properties = new JwtProperties();
        properties.setJwtSecret("unit-test-secret-with-at-least-32-characters");
        properties.setJwtAccessExpireMinutes(30);
        properties.setJwtRefreshExpireDays(7);
        jwt = new JwtService(properties);
    }

    @Test
    void accessContainsIdentityAndVersion() {
        String token = jwt.access("USER", 42L, 3);
        Claims claims = jwt.parse(token, JwtService.ACCESS);
        assertEquals("42", claims.getSubject());
        assertEquals("USER", claims.get("typ", String.class));
        assertEquals(3, claims.get("ver", Integer.class));
    }

    @Test
    void refreshCannotBeUsedAsAccess() {
        JwtService.GeneratedRefresh refresh = jwt.refresh("ADMIN", 7L, 0);
        assertThrows(ServiceException.class, () -> jwt.parse(refresh.token(), JwtService.ACCESS));
        assertEquals(64, jwt.hash(refresh.token()).length());
        assertNotEquals(refresh.token(), jwt.hash(refresh.token()));
    }
}
