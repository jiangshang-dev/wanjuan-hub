package com.heima.modules.payment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.common.PageResult;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.payment.dto.PaymentAdminDtos;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.entity.TradePaymentNotification;
import com.heima.modules.payment.entity.TradeReconciliation;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.mapper.TradePaymentNotificationMapper;
import com.heima.modules.payment.mapper.TradeReconciliationMapper;
import com.heima.modules.payment.service.PaymentAdminQueryService;
import com.heima.modules.payment.vo.PaymentAdminVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAdminQueryServiceTest {
    @Mock private TradePaymentMapper paymentMapper;
    @Mock private TradePaymentNotificationMapper notificationMapper;
    @Mock private TradeReconciliationMapper reconciliationMapper;
    @Mock private OrderMapper orderMapper;
    @InjectMocks private PaymentAdminQueryService service;

    @BeforeAll
    static void initTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TradeReconciliation.class);
    }

    @Test
    void paymentPageUsesRequestedPageAndResolvesOrderNumber() {
        TradePayment payment = new TradePayment();
        payment.setId(10L);
        payment.setPaymentNo("P1");
        payment.setOrderId(1L);
        payment.setAmount(new BigDecimal("12.34"));
        Page<TradePayment> page = new Page<>(2, 10, 21);
        page.setRecords(List.of(payment));
        when(paymentMapper.selectPage(any(), any())).thenReturn(page);
        when(orderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(order(1L, "O1")));

        PaymentAdminDtos.PaymentQuery query = new PaymentAdminDtos.PaymentQuery();
        query.setPage(2);
        query.setSize(10);
        PageResult<PaymentAdminVO.PaymentVO> result = service.paymentPage(query);

        assertEquals(21, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals("O1", result.getRecords().getFirst().getOrderNo());
        assertEquals(10L, result.getRecords().getFirst().getId());
    }

    @Test
    void reconciliationPageFiltersByResolvedOrderId() {
        when(orderMapper.selectOne(any())).thenReturn(order(7L, "O7"));
        TradeReconciliation reconciliation = new TradeReconciliation();
        reconciliation.setId(70L);
        reconciliation.setOrderId(7L);
        reconciliation.setReconciliationNo("C1");
        Page<TradeReconciliation> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(reconciliation));
        when(reconciliationMapper.selectPage(any(), any())).thenReturn(page);
        when(orderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(order(7L, "O7")));

        PaymentAdminDtos.ReconciliationQuery query = new PaymentAdminDtos.ReconciliationQuery();
        query.setOrderNo("O7");
        PageResult<PaymentAdminVO.ReconciliationVO> result = service.reconciliationPage(query);

        ArgumentCaptor<LambdaQueryWrapper> wrapper = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(reconciliationMapper).selectPage(any(), wrapper.capture());
        assertTrue(wrapper.getValue().getSqlSegment().contains("order_id"));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue(7L));
        assertEquals("O7", result.getRecords().getFirst().getOrderNo());
    }

    @Test
    void unknownOrderReturnsEmptyReconciliationPageWithoutQueryingRecords() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        PaymentAdminDtos.ReconciliationQuery query = new PaymentAdminDtos.ReconciliationQuery();
        query.setOrderNo("MISSING");

        PageResult<PaymentAdminVO.ReconciliationVO> result = service.reconciliationPage(query);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(reconciliationMapper, never()).selectPage(any(), any());
    }

    @Test
    void notificationDetailNeverSerializesSensitiveRawPayload() throws Exception {
        TradePaymentNotification notification = new TradePaymentNotification();
        notification.setId(3L);
        notification.setNotificationId("N1");
        notification.setRawPayload("{\"sign\":\"secret\",\"privateKey\":\"do-not-return\"}");
        notification.setProcessStatus("PROCESSED");
        when(notificationMapper.selectOne(any())).thenReturn(notification);

        PaymentAdminVO.NotificationVO result = service.notificationDetail("N1");
        String json = new ObjectMapper().writeValueAsString(result);

        assertEquals("N1", result.getNotificationId());
        assertFalse(json.contains("rawPayload"));
        assertFalse(json.contains("raw_payload"));
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("privateKey"));
        assertTrue(List.of(PaymentAdminVO.NotificationVO.class.getDeclaredFields()).stream()
                .noneMatch(field -> field.getName().toLowerCase().contains("payload")));
    }

    private static Order order(Long id, String orderNo) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        return order;
    }
}
