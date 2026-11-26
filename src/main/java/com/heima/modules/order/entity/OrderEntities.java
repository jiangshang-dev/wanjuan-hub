package com.heima.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class OrderEntities {
    private OrderEntities() {
    }

    @Data
    @TableName("trade_order")
    @EqualsAndHashCode(callSuper = true)
    public static class Order extends BaseEntity {
        private String orderNo;
        private String idempotencyKey;
        private Long userId;
        private String orderType;
        private Long resourceId;
        private Long membershipPlanId;
        private Integer membershipDurationDays;
        private Integer membershipDailyLimit;
        private BigDecimal totalAmount;
        private String currency;
        private String clientType;
        private String status;
        private String grantStatus;
        private LocalDateTime expireAt;
        private LocalDateTime paidAt;
        private LocalDateTime closedAt;
        private String closeReason;
        private Integer version;
    }

    @Data
    @TableName("trade_order_item")
    @EqualsAndHashCode(callSuper = true)
    public static class Item extends BaseEntity {
        private Long orderId;
        private Long resourceId;
        private String resourceName;
        private BigDecimal price;
        private Long ownerId;
    }

    @Data
    @TableName("trade_refund")
    @EqualsAndHashCode(callSuper = true)
    public static class Refund extends BaseEntity {
        private String refundNo;
        private Long orderId;
        private Long paymentId;
        private Long userId;
        private String channel;
        private BigDecimal amount;
        private String currency;
        private String reason;
        private String status;
        private String channelRefundNo;
        private String channelStatus;
        private Long reviewerId;
        private String reviewOpinion;
        private LocalDateTime reviewedAt;
        private LocalDateTime refundedAt;
        private Integer entitlementRevoked;
        private String errorMessage;
        private Integer version;
    }
}
