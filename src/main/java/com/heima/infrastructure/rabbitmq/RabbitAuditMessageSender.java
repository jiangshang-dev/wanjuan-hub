package com.heima.infrastructure.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.entity.AuditOutbox;
import com.heima.modules.audit.enums.OutboxStatus;
import com.heima.modules.audit.mapper.AuditOutboxMapper;
import com.heima.modules.audit.service.AuditMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RabbitAuditMessageSender implements AuditMessageSender {
    private final RabbitTemplate rabbitTemplate;
    private final AuditOutboxMapper outboxMapper;

    public RabbitAuditMessageSender(RabbitTemplate rabbitTemplate, AuditOutboxMapper outboxMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate.setMandatory(true);
        this.rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getCorrelationId();
            if (messageId != null) {
                markFailure(messageId, "消息无法路由：" + returned.getReplyText());
            }
        });
    }

    @Override
    public void send(AuditOutbox outbox) {
        CorrelationData correlation = new CorrelationData(outbox.getMessageId());
        correlation.getFuture().whenComplete((confirm, error) -> {
            if (error != null) {
                markFailure(outbox.getMessageId(), "发布确认异常");
            } else if (!confirm.isAck()) {
                markFailure(outbox.getMessageId(), "Broker拒绝消息：" + confirm.getReason());
            } else {
                outboxMapper.update(null, new LambdaUpdateWrapper<AuditOutbox>()
                        .eq(AuditOutbox::getMessageId, outbox.getMessageId())
                        .set(AuditOutbox::getStatus, OutboxStatus.SENT.name())
                        .set(AuditOutbox::getSentAt, LocalDateTime.now()).set(AuditOutbox::getLastError, null)
                        .set(AuditOutbox::getLeaseOwner, null).set(AuditOutbox::getLeaseUntil, null));
            }
        });
        try {
            rabbitTemplate.convertAndSend(AuditRabbitConstants.EXCHANGE, AuditRabbitConstants.ROUTING_KEY, outbox.getPayload(), message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setContentType("application/json");
                message.getMessageProperties().setCorrelationId(outbox.getMessageId());
                return message;
            }, correlation);
        } catch (RuntimeException e) {
            markFailure(outbox.getMessageId(), e.getMessage());
        }
    }

    private void markFailure(String messageId, String error) {
        String reason = error == null ? "消息投递失败" : error.length() > 500 ? error.substring(0, 500) : error;
        int baseDelay = 5;
        outboxMapper.update(null, new LambdaUpdateWrapper<AuditOutbox>().eq(AuditOutbox::getMessageId, messageId)
                .set(AuditOutbox::getStatus, OutboxStatus.FAILED.name()).set(AuditOutbox::getLastError, reason)
                .set(AuditOutbox::getNextRetryAt, LocalDateTime.now().plusSeconds(baseDelay))
                .set(AuditOutbox::getLeaseOwner, null).set(AuditOutbox::getLeaseUntil, null)
                .setSql("retry_count = retry_count + 1"));
        log.warn("Audit message delivery failed, messageId={}, reason={}", messageId, reason);
    }
}
