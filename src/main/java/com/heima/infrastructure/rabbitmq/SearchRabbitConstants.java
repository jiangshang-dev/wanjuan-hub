package com.heima.infrastructure.rabbitmq;

public final class SearchRabbitConstants {
    public static final String EXCHANGE = "wanjuan.search.exchange";
    public static final String QUEUE = "wanjuan.search.queue";
    public static final String ROUTING_KEY = "wanjuan.search.index";
    public static final String RETRY_EXCHANGE = "wanjuan.search.retry.exchange";
    public static final String RETRY_QUEUE = "wanjuan.search.retry.queue";
    public static final String RETRY_ROUTING_KEY = "wanjuan.search.retry";
    public static final String DEAD_EXCHANGE = "wanjuan.search.dlx";
    public static final String DEAD_QUEUE = "wanjuan.search.dlq";
    public static final String DEAD_ROUTING_KEY = "wanjuan.search.dead";

    private SearchRabbitConstants() {
    }
}
