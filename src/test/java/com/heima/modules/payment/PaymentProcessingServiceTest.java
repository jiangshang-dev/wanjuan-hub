package com.heima.modules.payment;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import com.heima.infrastructure.payment.PaymentGatewayRegistry;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.mapper.OrderItemMapper;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.order.service.OrderService;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.entity.TradePaymentNotification;
import com.heima.modules.payment.event.PaymentCompletedEvent;
import com.heima.modules.payment.event.PaymentEventPublisher;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.mapper.TradePaymentNotificationMapper;
import com.heima.modules.payment.mapper.TradeReconciliationMapper;
import com.heima.modules.payment.service.PaymentProcessingService;
import com.heima.modules.resource.service.ResourceTradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper itemMapper;
    @Mock private TradePaymentMapper paymentMapper;
    @Mock private TradePaymentNotificationMapper notificationMapper;
    @Mock private TradeReconciliationMapper reconciliationMapper;
    @Mock private PaymentGatewayRegistry gatewayRegistry;
    @Mock private ResourceTradeService resourceTradeService;
    @Mock private PaymentEventPublisher eventPublisher;
    @Mock private OrderService orderService;
    @InjectMocks private PaymentProcessingService service;

    private Order order;
    private TradePayment payment;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNo("O1");
        order.setUserId(2L);
        order.setResourceId(3L);
        order.setTotalAmount(new BigDecimal("12.34"));
        order.setCurrency("CNY");
        order.setStatus("PENDING_PAYMENT");
        order.setGrantStatus("NONE");
        payment = new TradePayment();
        payment.setId(10L);
        payment.setOrderId(1L);
        payment.setChannel("ALIPAY");
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("CNY");
        payment.setStatus("PAYING");
    }

    @Test
    void paidNotificationGrantsOnceAndPublishesPaid() {
        mockUnseenNotification();
        mockLockedPayment();
        when(paymentMapper.selectByChannelTradeNo("ALIPAY", "T1")).thenReturn(null);

        service.handleNotification("ALIPAY", notification("12.34", true));

        verify(resourceTradeService).grantPaidAccess(2L, 3L, "O1");
        assertEquals("PAID", order.getStatus());
        assertEquals("GRANTED", order.getGrantStatus());
        assertEquals("SUCCEEDED", payment.getStatus());
        ArgumentCaptor<PaymentCompletedEvent> event = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertEquals("PAID", event.getValue().action());
        assertEquals("O1", event.getValue().orderNo());
        assertEquals(2L, event.getValue().userId());
        assertEquals(3L, event.getValue().resourceId());
        assertEquals(order.getTotalAmount(), event.getValue().amount());
    }

    @Test
    void duplicateNotificationDoesNotGrantOrPublishAgain() {
        when(notificationMapper.selectOne(any())).thenReturn(new TradePaymentNotification());

        service.handleNotification("ALIPAY", notification("12.34", true));

        verifyNoInteractions(orderMapper, paymentMapper, resourceTradeService, eventPublisher);
    }

    @Test
    void wrongAmountIsRejectedWithoutGrant() {
        mockUnseenNotification();
        mockLockedPayment();

        assertThrows(ServiceException.class, () -> service.handleNotification("ALIPAY", notification("12.35", true)));

        verifyNoInteractions(resourceTradeService, eventPublisher);
        assertEquals("PENDING_PAYMENT", order.getStatus());
        assertEquals("NONE", order.getGrantStatus());
    }

    @Test
    void unsuccessfulNotificationNeverGrants() {
        mockUnseenNotification();
        mockLockedPayment();

        service.handleNotification("ALIPAY", notification("12.34", false));

        verifyNoInteractions(resourceTradeService, eventPublisher);
        assertEquals("PENDING_PAYMENT", order.getStatus());
        assertEquals("PAYING", payment.getStatus());
    }

    @Test
    void recoveryGrantsOnlyPendingPaidOrder() {
        order.setStatus("PAID");
        order.setGrantStatus("PENDING");
        when(orderMapper.selectForUpdate("O1")).thenReturn(order);

        service.recoverGrant("O1");

        verify(resourceTradeService).grantPaidAccess(2L, 3L, "O1");
        verify(eventPublisher).publish(any(PaymentCompletedEvent.class));

        order.setGrantStatus("GRANTED");
        service.recoverGrant("O1");
        verify(resourceTradeService).grantPaidAccess(2L, 3L, "O1");
        verify(eventPublisher).publish(any(PaymentCompletedEvent.class));
    }

    private void mockUnseenNotification() {
        when(notificationMapper.selectOne(any())).thenReturn(null);
    }

    private void mockLockedPayment() {
        when(orderMapper.selectForUpdate("O1")).thenReturn(order);
        when(paymentMapper.selectByOrderForUpdate(1L)).thenReturn(payment);
    }

    private PaymentNotification notification(String amount, boolean success) {
        return new PaymentNotification("N1", "O1", "T1", success ? "TRADE_SUCCESS" : "WAIT_BUYER_PAY",
                new BigDecimal(amount), "CNY", "app", null, success, "{}");
    }
}
