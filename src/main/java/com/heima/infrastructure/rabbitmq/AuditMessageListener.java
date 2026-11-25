package com.heima.infrastructure.rabbitmq;

import com.heima.modules.audit.dto.AuditMessage;
import com.heima.modules.audit.service.AuditService;
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
public class AuditMessageListener {
    private static final int MAX_ATTEMPTS = 3;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final AuditService auditService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = AuditRabbitConstants.QUEUE)
    public void onMessage(String payload, Message message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            auditService.consume(JSON.readValue(payload, AuditMessage.class));
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            int attempt = retryCount(message) + 1;
            message.getMessageProperties().setHeader("x-retry-count", attempt);
            String targetExchange = attempt >= MAX_ATTEMPTS ? AuditRabbitConstants.DEAD_EXCHANGE : AuditRabbitConstants.RETRY_EXCHANGE;
            String routingKey = attempt >= MAX_ATTEMPTS ? AuditRabbitConstants.DEAD_ROUTING_KEY : AuditRabbitConstants.RETRY_ROUTING_KEY;
            log.warn("Audit consume failed, attempt={}, target={}", attempt, targetExchange, e);
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
