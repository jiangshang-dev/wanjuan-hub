package com.heima.modules.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {
    private final ApplicationEventPublisher delegate;

    public void publish(PaymentCompletedEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(event);
                }
            });
            return;
        }
        publishSafely(event);
    }

    private void publishSafely(PaymentCompletedEvent event) {
        try {
            delegate.publishEvent(event);
        } catch (RuntimeException e) {
            log.error("支付完成事件发布失败，action={}", event.action(), e);
        }
    }
}
