package com.heima.modules.auth.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.location.IpLocationService;
import com.heima.infrastructure.redis.RedisRateLimiter;
import com.heima.modules.auth.dto.AuthDtos.EmailLoginRequest;
import com.heima.modules.auth.dto.AuthDtos.TokenResponse;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.LoginLogMapper;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.auth.mapper.UserProfileMapper;
import com.heima.modules.file.service.FileService;
import com.heima.modules.system.mapper.SysAdminMapper;
import com.heima.security.config.SecurityProperties;
import com.heima.security.service.RefreshTokenService;
import com.heima.security.token.JwtService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserAccountMapper userMapper;
    @Mock private UserProfileMapper profileMapper;
    @Mock private SysAdminMapper adminMapper;
    @Mock private LoginLogMapper loginLogMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CaptchaService captchaService;
    @Mock private VerificationCodeService codeService;
    @Mock private RedisRateLimiter limiter;
    @Mock private RefreshTokenService refreshService;
    @Mock private JwtService jwt;
    @Mock private SecurityProperties securityProperties;
    @Mock private IpLocationService locationService;
    @Mock private FileService fileService;
    @InjectMocks private AuthService authService;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserAccount.class);
    }

    @BeforeEach
    void setUpTokens() {
        lenient().when(jwt.accessExpiresIn()).thenReturn(3600L);
        lenient().when(refreshService.issue(anyString(), anyLong(), anyInt())).thenAnswer(invocation ->
                new RefreshTokenService.IssuedTokens("access", "refresh", "family", "token"));
    }

    @Test
    void unregisteredEmailAutomaticallyCreatesAccountAndProfile() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserAccount.class).setId(42L);
            return 1;
        });

        TokenResponse response = authService.loginByEmail(request("New.User@Example.com"));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userMapper).insert(userCaptor.capture());
        UserAccount created = userCaptor.getValue();
        assertEquals("new.user@example.com", created.getEmail());
        assertEquals("encoded-password", created.getPassword());
        assertEquals(1, created.getStatus());
        assertEquals(0, created.getTokenVersion());
        assertTrue(created.getUsername().startsWith("new_user_"));

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).insert(profileCaptor.capture());
        assertEquals(42L, profileCaptor.getValue().getUserId());
        assertEquals("new_user", profileCaptor.getValue().getNickname());
        assertEquals("access", response.accessToken());
        verify(codeService).consumeEmail("LOGIN", "new.user@example.com", "123456");
    }

    @Test
    void registeredEmailLogsInWithoutCreatingAnotherAccount() {
        UserAccount existing = existingUser(1);
        when(userMapper.selectOne(any())).thenReturn(existing);

        TokenResponse response = authService.loginByEmail(request("User@Example.com"));

        assertEquals("access", response.accessToken());
        verify(userMapper, never()).insert(any(UserAccount.class));
        verify(profileMapper, never()).insert(any(UserProfile.class));
        verify(refreshService).issue("USER", 7L, 0);
    }

    @Test
    void disabledEmailAccountIsStillRejected() {
        UserAccount existing = existingUser(0);
        when(userMapper.selectOne(any())).thenReturn(existing);

        ServiceException error = assertThrows(ServiceException.class, () -> authService.loginByEmail(request("user@example.com")));

        assertEquals(403, error.getCode());
        verify(userMapper, never()).insert(any(UserAccount.class));
        verify(refreshService, never()).issue(anyString(), anyLong(), anyInt());
    }

    private UserAccount existingUser(int status) {
        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setUsername("existing");
        user.setEmail("user@example.com");
        user.setStatus(status);
        user.setTokenVersion(0);
        return user;
    }

    private EmailLoginRequest request(String email) {
        return new EmailLoginRequest(email, "123456", "captcha-id", "captcha-code");
    }
}
