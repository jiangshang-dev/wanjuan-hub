package com.heima.common.aspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationLogAspectTest {
    @Test
    void masksAllSensitiveParameters() {
        String raw = "LoginRequest[account=a,password=plain,captchaId=id,captchaCode=1234,accessToken=abc,clientSecret=xyz]";
        String masked = OperationLogAspect.mask(raw);
        assertFalse(masked.contains("plain"));
        assertFalse(masked.contains("1234"));
        assertFalse(masked.contains("abc"));
        assertFalse(masked.contains("xyz"));
        assertTrue(masked.contains("account=a"));
        assertTrue(masked.contains("captchaId=id"));
    }
}
