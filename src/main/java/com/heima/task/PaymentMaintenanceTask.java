package com.heima.task;

import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.entity.OrderEntities.Refund;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.order.mapper.TradeRefundMapper;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.service.PaymentProcessingService;
import com.heima.modules.payment.service.RefundProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMaintenanceTask {
    private final OrderMapper orderMapper;
    private final TradePaymentMapper paymentMapper;
    private final TradeRefundMapper refundMapper;
    private final PaymentProcessingService paymentService;
    private final RefundProcessingService refundService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void closeExpiredOrders() {
        for (Order order : orderMapper.selectExpired(LocalDateTime.now(), 100)) {
            try {
                paymentService.close(order.getOrderNo(), null, "支付超时自动关单");
            } catch (RuntimeException e) {
                log.warn("超时订单关单未完成，orderNo={}，message={}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 90_000)
    public void queryPendingPayments() {
        for (TradePayment payment : paymentMapper.selectNeedQuery(LocalDateTime.now().minusMinutes(2), 100)) {
            reconcile(payment, "TASK");
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
    public void auditSucceededPayments() {
        for (TradePayment payment : paymentMapper.selectSucceededForAudit(LocalDateTime.now().minusHours(1), 100)) {
            reconcile(payment, "AUDIT");
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 75_000)
    public void synchronizeRefunds() {
        for (Refund refund : refundMapper.selectPendingChannelSync(100)) {
            try {
                refundService.synchronize(refund.getRefundNo());
            } catch (RuntimeException e) {
                log.warn("退款渠道同步未完成，refundNo={}，message={}", refund.getRefundNo(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 105_000)
    public void recoverPendingGrants() {
        for (Order order : orderMapper.selectPendingGrants(100)) {
            try {
                paymentService.recoverGrant(order.getOrderNo());
            } catch (RuntimeException e) {
                log.warn("支付授权恢复未完成，orderNo={}，message={}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    private void reconcile(TradePayment payment, String trigger) {
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null) {
            return;
        }
        try {
            paymentService.reconcile(order.getOrderNo(), trigger, null);
        } catch (RuntimeException e) {
            log.warn("支付主动对账未完成，orderNo={}，message={}", order.getOrderNo(), e.getMessage());
        }
    }
}
