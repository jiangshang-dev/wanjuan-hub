package com.heima.modules.message.event;

public record UserNotificationEvent(
        Long userId,
        String type,
        String title,
        String content,
        String bizType,
        String bizId,
        String dedupKey) {
}
