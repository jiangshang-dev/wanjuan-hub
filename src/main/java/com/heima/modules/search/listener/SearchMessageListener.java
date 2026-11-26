package com.heima.modules.search.listener;

import com.heima.infrastructure.rabbitmq.SearchRabbitConstants;
import com.heima.modules.search.event.SearchIndexMessage;
import com.heima.modules.search.service.SearchIndexService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchMessageListener {
    private static final int MAX_ATTEMPTS = 3;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final SearchIndexService searchIndexService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = SearchRabbitConstants.QUEUE)
    public void onMessage(String payload, Message message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            searchIndexService.consume(JSON.readValue(payload, SearchIndexMessage.class));
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            int attempt = retryCount(message) + 1;
            message.getMessageProperties().setHeader("x-retry-count", attempt);
            boolean dead = attempt >= MAX_ATTEMPTS;
            String targetExchange = dead ? SearchRabbitConstants.DEAD_EXCHANGE : SearchRabbitConstants.RETRY_EXCHANGE;
            String routingKey = dead ? SearchRabbitConstants.DEAD_ROUTING_KEY : SearchRabbitConstants.RETRY_ROUTING_KEY;
            log.warn("Search consume failed, attempt={}, target={}", attempt, targetExchange, ex);
            try {
                rabbitTemplate.send(targetExchange, routingKey, message);
                channel.basicAck(deliveryTag, false);
            } catch (RuntimeException | IOException publishFailure) {
                channel.basicNack(deliveryTag, false, false);
                throw publishFailure;
            }
        }
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeader("x-retry-count");
        return value instanceof Number number ? number.intValue() : 0;
    }
}
