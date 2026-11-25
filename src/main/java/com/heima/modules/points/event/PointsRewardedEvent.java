package com.heima.modules.points.event;

public record PointsRewardedEvent(Long userId, String bizType, Long bizId, Integer points, String idempotencyKey) {
}
