package com.heima.modules.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trade_reconciliation")
@EqualsAndHashCode(callSuper = true)
public class TradeReconciliation extends BaseEntity {
    private String reconciliationNo;
    private Long orderId;
    private Long paymentId;
    private String channel;
    private String triggeredBy;
    private String result;
    private BigDecimal expectedAmount;
    private BigDecimal channelAmount;
    private String expectedStatus;
    private String channelStatus;
    private String channelTradeNo;
    private Long operatorId;
    private String detail;
    private LocalDateTime reconciledAt;
}
