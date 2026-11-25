package com.heima.modules.payment;

import com.heima.infrastructure.payment.PaymentChannelGateway;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelRefund;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundNotification;
import com.heima.infrastructure.payment.PaymentGatewayRegistry;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.entity.OrderEntities.Refund;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.order.mapper.TradeRefundMapper;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.entity.TradePaymentNotification;
import com.heima.modules.payment.event.PaymentCompletedEvent;
import com.heima.modules.payment.event.PaymentEventPublisher;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.mapper.TradePaymentNotificationMapper;
import com.heima.modules.payment.service.RefundProcessingService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundProcessingServiceTest {
    @Mock private TradeRefundMapper refundMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private TradePaymentMapper paymentMapper;
    @Mock private TradePaymentNotificationMapper notificationMapper;
    @Mock private PaymentGatewayRegistry gatewayRegistry;
    @Mock private ResourceTradeService resourceTradeService;
    @Mock private PaymentEventPublisher eventPublisher;
    @Mock private PaymentChannelGateway gateway;
    @InjectMocks private RefundProcessingService service;

    private Refund refund;
    private Order order;
    private TradePayment payment;

    @BeforeEach
    void setUp() {
        refund = new Refund();
        refund.setId(20L);
        refund.setRefundNo("R1");
        refund.setOrderId(1L);
        refund.setPaymentId(10L);
        refund.setUserId(2L);
        refund.setChannel("ALIPAY");
        refund.setAmount(new BigDecimal("12.34"));
        refund.setCurrency("CNY");
        refund.setReason("不再需要");
        refund.setStatus("PENDING_REVIEW");
        refund.setEntitlementRevoked(0);
        order = new Order();
        order.setId(1L);
        order.setOrderNo("O1");
        order.setUserId(2L);
        order.setResourceId(3L);
        order.setTotalAmount(refund.getAmount());
        order.setStatus("REFUNDING");
        order.setGrantStatus("GRANTED");
        payment = new TradePayment();
        payment.setId(10L);
        payment.setOrderId(1L);
        payment.setChannel("ALIPAY");
        payment.setChannelTradeNo("T1");
        payment.setStatus("SUCCEEDED");
    }

    @Test
    void realRefundSuccessRevokesAndPublishesRefunded() {
        mockReviewContext();
        when(gatewayRegistry.get("ALIPAY")).thenReturn(gateway);
        when(gateway.createRefund(any())).thenReturn(new ChannelRefund("R1", "O1", "T1", "CR1",
                "REFUND_SUCCESS", refund.getAmount(), "CNY", true));
        OrderDtos.RefundReviewRequest request = new OrderDtos.RefundReviewRequest();
        request.setAction("APPROVE");

        service.review(20L, request, 99L);

        verify(resourceTradeService).revokePaidAccess(2L, 3L, "REFUND:R1");
        assertEquals("SUCCEEDED", refund.getStatus());
        assertEquals(1, refund.getEntitlementRevoked());
        assertEquals("REFUNDED", order.getStatus());
        assertEquals("REVOKED", order.getGrantStatus());
        assertEquals("REFUNDED", payment.getStatus());
        ArgumentCaptor<PaymentCompletedEvent> event = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertEquals("REFUNDED", event.getValue().action());
        assertEquals("O1", event.getValue().orderNo());
        assertEquals(refund.getAmount(), event.getValue().amount());
    }

    @Test
    void duplicateRefundNotificationDoesNotRevokeAgain() {
        when(notificationMapper.selectOne(any())).thenReturn(new TradePaymentNotification());
        RefundNotification notification = new RefundNotification("RN1", "R1", "O1", "T1", "CR1",
                "REFUND.SUCCESS", "SUCCESS", refund.getAmount(), "CNY", true, "{}");

        service.handleNotification("ALIPAY", notification);

        verifyNoInteractions(refundMapper, orderMapper, paymentMapper, resourceTradeService, eventPublisher);
    }

    private void mockReviewContext() {
        when(refundMapper.selectForUpdate(20L)).thenReturn(refund);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.selectForUpdate("O1")).thenReturn(order);
        when(paymentMapper.selectByOrderForUpdate(1L)).thenReturn(payment);
    }
}
