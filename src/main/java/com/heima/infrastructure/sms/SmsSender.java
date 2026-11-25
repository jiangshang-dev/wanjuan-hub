package com.heima.infrastructure.sms;

public interface SmsSender {
    void sendCode(String phone, String scene, String code);
    default boolean isAvailable() { return true; }
}
