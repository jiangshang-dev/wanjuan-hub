package com.heima.security.service;

import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.RefreshTokenEntity;
import com.heima.modules.auth.mapper.RefreshTokenMapper;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.system.mapper.SysAdminMapper;
import com.heima.security.token.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private RefreshTokenMapper mapper;
    private JwtService jwt;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        mapper = mock(RefreshTokenMapper.class);
        jwt = mock(JwtService.class);
        service = new RefreshTokenService(mapper, mock(UserAccountMapper.class), mock(SysAdminMapper.class), jwt);
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("token-id");
        when(claims.getSubject()).thenReturn("42");
        when(claims.get("typ", String.class)).thenReturn("USER");
        when(jwt.parse("raw", JwtService.REFRESH)).thenReturn(claims);
        when(jwt.hash("raw")).thenReturn("hash");
    }

    @Test
    void reusedTokenRevokesWholeFamily() {
        RefreshTokenEntity old = oldToken();
        old.setRevokedAt(LocalDateTime.now());
        when(mapper.selectOne(any())).thenReturn(old);
        assertThrows(ServiceException.class, () -> service.rotate("raw", "USER"));
        verify(mapper).revokeFamily(eq("family"), any());
        verify(mapper, never()).insert(any(RefreshTokenEntity.class));
    }

    @Test
    void missingStoredTokenIsRejected() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.rotate("raw", "USER"));
        verify(mapper, never()).rotate(any(), any(), any());
    }

    private RefreshTokenEntity oldToken() {
        RefreshTokenEntity old = new RefreshTokenEntity();
        old.setTokenId("token-id");
        old.setTokenHash("hash");
        old.setFamilyId("family");
        old.setUserType("USER");
        old.setUserId(42L);
        old.setExpiresAt(LocalDateTime.now().plusDays(1));
        return old;
    }
}
