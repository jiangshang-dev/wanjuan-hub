package com.heima.infrastructure.rabbitmq;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchRabbitConfig {
    @Bean
    public Declarables searchDeclarables() {
        DirectExchange exchange = new DirectExchange(SearchRabbitConstants.EXCHANGE, true, false);
        DirectExchange retryExchange = new DirectExchange(SearchRabbitConstants.RETRY_EXCHANGE, true, false);
        DirectExchange deadExchange = new DirectExchange(SearchRabbitConstants.DEAD_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(SearchRabbitConstants.QUEUE)
                .deadLetterExchange(SearchRabbitConstants.DEAD_EXCHANGE)
                .deadLetterRoutingKey(SearchRabbitConstants.DEAD_ROUTING_KEY).build();
        Queue retryQueue = QueueBuilder.durable(SearchRabbitConstants.RETRY_QUEUE)
                .deadLetterExchange(SearchRabbitConstants.EXCHANGE)
                .deadLetterRoutingKey(SearchRabbitConstants.ROUTING_KEY)
                .withArgument("x-message-ttl", 10_000).build();
        Queue deadQueue = QueueBuilder.durable(SearchRabbitConstants.DEAD_QUEUE).build();
        return new Declarables(exchange, retryExchange, deadExchange, queue, retryQueue, deadQueue,
                BindingBuilder.bind(queue).to(exchange).with(SearchRabbitConstants.ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retryExchange).with(SearchRabbitConstants.RETRY_ROUTING_KEY),
                BindingBuilder.bind(deadQueue).to(deadExchange).with(SearchRabbitConstants.DEAD_ROUTING_KEY));
    }
}
