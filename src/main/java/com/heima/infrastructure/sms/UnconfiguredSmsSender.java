package com.heima.infrastructure.sms;

import com.heima.exception.ServiceException;
import org.springframework.stereotype.Component;

@Component
public class UnconfiguredSmsSender implements SmsSender {
    @Override public boolean isAvailable() { return false; }
    @Override
    public void sendCode(String phone, String scene, String code) {
        throw new ServiceException(503,"短信服务未配置");
    }
}
