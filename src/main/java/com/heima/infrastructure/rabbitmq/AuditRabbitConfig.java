package com.heima.infrastructure.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditRabbitConfig {
    @Bean
    public Declarables auditDeclarables() {
        DirectExchange exchange = new DirectExchange(AuditRabbitConstants.EXCHANGE, true, false);
        DirectExchange retryExchange = new DirectExchange(AuditRabbitConstants.RETRY_EXCHANGE, true, false);
        DirectExchange deadExchange = new DirectExchange(AuditRabbitConstants.DEAD_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(AuditRabbitConstants.QUEUE)
                .deadLetterExchange(AuditRabbitConstants.DEAD_EXCHANGE)
                .deadLetterRoutingKey(AuditRabbitConstants.DEAD_ROUTING_KEY).build();
        Queue retryQueue = QueueBuilder.durable(AuditRabbitConstants.RETRY_QUEUE)
                .deadLetterExchange(AuditRabbitConstants.EXCHANGE)
                .deadLetterRoutingKey(AuditRabbitConstants.ROUTING_KEY)
                .withArgument("x-message-ttl", 10_000).build();
        Queue deadQueue = QueueBuilder.durable(AuditRabbitConstants.DEAD_QUEUE).build();
        return new Declarables(exchange, retryExchange, deadExchange, queue, retryQueue, deadQueue,
                BindingBuilder.bind(queue).to(exchange).with(AuditRabbitConstants.ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retryExchange).with(AuditRabbitConstants.RETRY_ROUTING_KEY),
                BindingBuilder.bind(deadQueue).to(deadExchange).with(AuditRabbitConstants.DEAD_ROUTING_KEY));
    }
}
