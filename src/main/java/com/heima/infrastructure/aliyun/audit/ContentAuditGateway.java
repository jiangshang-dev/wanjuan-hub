package com.heima.infrastructure.aliyun.audit;

public interface ContentAuditGateway {
    AuditGatewayResult reviewText(String content);
    AuditGatewayResult reviewImage(String imageUrl);
}
