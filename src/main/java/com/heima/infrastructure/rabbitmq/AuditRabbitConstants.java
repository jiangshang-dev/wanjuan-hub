package com.heima.infrastructure.rabbitmq;

public final class AuditRabbitConstants {
    public static final String EXCHANGE = "wanjuan.audit.exchange";
    public static final String QUEUE = "wanjuan.audit.queue";
    public static final String ROUTING_KEY = "wanjuan.audit.process";
    public static final String RETRY_EXCHANGE = "wanjuan.audit.retry.exchange";
    public static final String RETRY_QUEUE = "wanjuan.audit.retry.queue";
    public static final String RETRY_ROUTING_KEY = "wanjuan.audit.retry";
    public static final String DEAD_EXCHANGE = "wanjuan.audit.dlx";
    public static final String DEAD_QUEUE = "wanjuan.audit.dlq";
    public static final String DEAD_ROUTING_KEY = "wanjuan.audit.dead";

    private AuditRabbitConstants() {
    }
}
