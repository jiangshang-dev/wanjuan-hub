package com.heima.modules.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentAdminVO {
    private PaymentAdminVO() {
    }

    @Data
    @Schema(description = "后台支付流水")
    public static class PaymentVO {
        @Schema(description = "支付流水ID") private Long id;
        @Schema(description = "支付单号") private String paymentNo;
        @Schema(description = "订单ID") private Long orderId;
        @Schema(description = "订单号") private String orderNo;
        @Schema(description = "支付渠道") private String channel;
        @Schema(description = "支付场景") private String scene;
        @Schema(description = "支付金额") private BigDecimal amount;
        @Schema(description = "币种") private String currency;
        @Schema(description = "支付状态") private String status;
        @Schema(description = "渠道交易号") private String channelTradeNo;
        @Schema(description = "渠道状态") private String channelStatus;
        @Schema(description = "支付完成时间") private LocalDateTime paidAt;
        @Schema(description = "最近主动查单时间") private LocalDateTime lastQueryAt;
        @Schema(description = "渠道错误信息") private String errorMessage;
        @Schema(description = "乐观锁版本号") private Integer version;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }

    @Data
    @Schema(description = "后台支付通知元数据，不包含敏感原始报文")
    public static class NotificationVO {
        @Schema(description = "通知记录ID") private Long id;
        @Schema(description = "通知ID") private String notificationId;
        @Schema(description = "支付渠道") private String channel;
        @Schema(description = "支付流水ID") private Long paymentId;
        @Schema(description = "订单ID") private Long orderId;
        @Schema(description = "订单号") private String orderNo;
        @Schema(description = "渠道交易号") private String channelTradeNo;
        @Schema(description = "通知事件类型") private String eventType;
        @Schema(description = "处理状态") private String processStatus;
        @Schema(description = "处理错误信息") private String errorMessage;
        @Schema(description = "处理完成时间") private LocalDateTime processedAt;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }

    @Data
    @Schema(description = "后台支付对账记录")
    public static class ReconciliationVO {
        @Schema(description = "对账记录ID") private Long id;
        @Schema(description = "对账号") private String reconciliationNo;
        @Schema(description = "订单ID") private Long orderId;
        @Schema(description = "订单号") private String orderNo;
        @Schema(description = "支付流水ID") private Long paymentId;
        @Schema(description = "支付渠道") private String channel;
        @Schema(description = "触发方式") private String triggeredBy;
        @Schema(description = "对账结果") private String result;
        @Schema(description = "本地订单金额") private BigDecimal expectedAmount;
        @Schema(description = "渠道支付金额") private BigDecimal channelAmount;
        @Schema(description = "本地订单状态") private String expectedStatus;
        @Schema(description = "渠道支付状态") private String channelStatus;
        @Schema(description = "渠道交易号") private String channelTradeNo;
        @Schema(description = "操作管理员ID") private Long operatorId;
        @Schema(description = "对账说明") private String detail;
        @Schema(description = "对账时间") private LocalDateTime reconciledAt;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }
}
