package com.heima.modules.payment;

import com.heima.modules.payment.event.PaymentCompletedEvent;
import com.heima.modules.payment.event.PaymentEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentEventPublisherTest {
    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesOnlyAfterLocalTransactionCommits() {
        ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
        PaymentEventPublisher publisher = new PaymentEventPublisher(delegate);
        PaymentCompletedEvent event = new PaymentCompletedEvent("O1", 2L, 3L, new BigDecimal("1.00"), "PAID");
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish(event);

        verify(delegate, never()).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(delegate).publishEvent(event);
    }
}
