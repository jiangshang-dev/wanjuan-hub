package com.heima.modules.message.listener;

import com.heima.modules.audit.entity.AuditTask;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.audit.mapper.AuditTaskMapper;
import com.heima.modules.message.service.MessageService;
import com.heima.modules.payment.event.PaymentCompletedEvent;
import com.heima.modules.points.event.PointsRewardedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {
    private final MessageService messageService;
    private final AuditTaskMapper auditTaskMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuditCompleted(AuditCompletedEvent event) {
        try {
            AuditTask task = auditTaskMapper.selectById(event.taskId());
            if (task == null || task.getAuthorId() == null) return;
            String title = switch (event.status()) {
                case APPROVED -> "内容审核通过";
                case REJECTED -> "内容审核未通过";
                case MANUAL_REVIEW -> "内容已转人工审核";
                default -> "内容审核状态更新";
            };
            String content = event.reason() == null || event.reason().isBlank()
                    ? "你的内容审核状态已更新为" + event.status().name()
                    : "审核结果：" + event.reason();
            messageService.notify(task.getAuthorId(), "AUDIT", title, content,
                    event.bizType(), String.valueOf(event.bizId()), "AUDIT_NOTICE:" + event.taskId());
        } catch (Exception ex) {
            log.error("Failed to send audit notification, taskId={}", event.taskId(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!"PAID".equals(event.action()) && !"REFUNDED".equals(event.action())) return;
        try {
            boolean paid = "PAID".equals(event.action());
            messageService.notify(event.userId(), "PAYMENT", paid ? "支付成功" : "退款成功",
                    (paid ? "订单支付成功，金额：" : "订单退款成功，金额：") + event.amount().toPlainString(),
                    "TRADE_ORDER", event.orderNo(), "PAYMENT_NOTICE:" + event.action() + ":" + event.orderNo());
        } catch (Exception ex) {
            log.error("Failed to send payment notification, orderNo={}", event.orderNo(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPointsRewarded(PointsRewardedEvent event) {
        try {
            messageService.notify(event.userId(), "POINTS", "积分到账",
                    "你获得" + event.points() + "积分", event.bizType(), String.valueOf(event.bizId()),
                    "POINTS_NOTICE:" + event.idempotencyKey());
        } catch (Exception ex) {
            log.error("Failed to send points notification, userId={}", event.userId(), ex);
        }
    }
}
