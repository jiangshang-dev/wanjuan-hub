package com.heima.infrastructure.aliyun.audit;

public record AuditGatewayResult(GatewayDecision decision, String reason, String requestId) {
}
