package com.heima.modules.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("trade_payment_notification")
@EqualsAndHashCode(callSuper = true)
public class TradePaymentNotification extends BaseEntity {
    private String notificationId;
    private String channel;
    private Long paymentId;
    private Long orderId;
    private String channelTradeNo;
    private String eventType;
    private String processStatus;
    private String rawPayload;
    private String errorMessage;
    private LocalDateTime processedAt;
}
