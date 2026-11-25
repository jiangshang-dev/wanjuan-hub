package com.heima.infrastructure.rabbitmq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.OutboxStatus;
import com.heima.modules.search.entity.SearchOutbox;
import com.heima.modules.search.mapper.SearchOutboxMapper;
import com.heima.modules.search.service.SearchMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitSearchMessageSender implements SearchMessageSender {
    private final RabbitTemplate rabbitTemplate;
    private final SearchOutboxMapper outboxMapper;

    @Override
    public void send(SearchOutbox outbox) {
        CorrelationData correlation = new CorrelationData(outbox.getMessageId());
        correlation.getFuture().whenComplete((confirm, error) -> {
            if (error != null) {
                markFailure(outbox.getMessageId(), "发布确认异常");
            } else if (!confirm.isAck()) {
                markFailure(outbox.getMessageId(), "Broker拒绝消息：" + confirm.getReason());
            } else {
                outboxMapper.update(null, new LambdaUpdateWrapper<SearchOutbox>()
                        .eq(SearchOutbox::getMessageId, outbox.getMessageId())
                        .set(SearchOutbox::getStatus, OutboxStatus.SENT.name())
                        .set(SearchOutbox::getSentAt, LocalDateTime.now()).set(SearchOutbox::getLastError, null)
                        .set(SearchOutbox::getLeaseOwner, null).set(SearchOutbox::getLeaseUntil, null));
            }
        });
        try {
            rabbitTemplate.convertAndSend(SearchRabbitConstants.EXCHANGE, SearchRabbitConstants.ROUTING_KEY,
                    outbox.getPayload(), message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setContentType("application/json");
                        message.getMessageProperties().setCorrelationId(outbox.getMessageId());
                        return message;
                    }, correlation);
        } catch (RuntimeException ex) {
            markFailure(outbox.getMessageId(), ex.getMessage());
        }
    }

    private void markFailure(String messageId, String error) {
        String reason = error == null ? "消息投递失败" : error.length() > 500 ? error.substring(0, 500) : error;
        outboxMapper.update(null, new LambdaUpdateWrapper<SearchOutbox>().eq(SearchOutbox::getMessageId, messageId)
                .set(SearchOutbox::getStatus, OutboxStatus.FAILED.name()).set(SearchOutbox::getLastError, reason)
                .set(SearchOutbox::getNextRetryAt, LocalDateTime.now().plusSeconds(5))
                .set(SearchOutbox::getLeaseOwner, null).set(SearchOutbox::getLeaseUntil, null)
                .setSql("retry_count = retry_count + 1"));
        log.warn("Search message delivery failed, messageId={}, reason={}", messageId, reason);
    }
}
