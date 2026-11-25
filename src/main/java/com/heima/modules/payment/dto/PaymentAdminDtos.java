package com.heima.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class PaymentAdminDtos {
    private PaymentAdminDtos() {
    }

    @Data
    @Schema(description = "后台支付流水分页查询")
    public static class PaymentQuery {
        @Min(1)
        @Schema(description = "页码", defaultValue = "1")
        private long page = 1;

        @Min(1)
        @Max(100)
        @Schema(description = "每页数量，最大100", defaultValue = "20")
        private long size = 20;

        @Size(max = 64)
        @Schema(description = "支付单号，精确查询")
        private String paymentNo;

        @Size(max = 64)
        @Schema(description = "订单号，精确查询")
        private String orderNo;

        @Pattern(regexp = "ALIPAY|WECHAT")
        @Schema(description = "支付渠道", allowableValues = {"ALIPAY", "WECHAT"})
        private String channel;

        @Pattern(regexp = "CREATED|PAYING|SUCCEEDED|CLOSED|REFUNDED")
        @Schema(description = "支付状态", allowableValues = {"CREATED", "PAYING", "SUCCEEDED", "CLOSED", "REFUNDED"})
        private String status;
    }

    @Data
    @Schema(description = "后台支付通知分页查询")
    public static class NotificationQuery {
        @Min(1)
        @Schema(description = "页码", defaultValue = "1")
        private long page = 1;

        @Min(1)
        @Max(100)
        @Schema(description = "每页数量，最大100", defaultValue = "20")
        private long size = 20;

        @Size(max = 192)
        @Schema(description = "通知ID，精确查询")
        private String notificationId;

        @Size(max = 64)
        @Schema(description = "订单号，精确查询")
        private String orderNo;

        @Pattern(regexp = "ALIPAY|WECHAT")
        @Schema(description = "支付渠道", allowableValues = {"ALIPAY", "WECHAT"})
        private String channel;

        @Pattern(regexp = "RECEIVED|PROCESSED|IGNORED")
        @Schema(description = "处理状态", allowableValues = {"RECEIVED", "PROCESSED", "IGNORED"})
        private String processStatus;
    }

    @Data
    @Schema(description = "后台支付对账记录分页查询")
    public static class ReconciliationQuery {
        @Min(1)
        @Schema(description = "页码", defaultValue = "1")
        private long page = 1;

        @Min(1)
        @Max(100)
        @Schema(description = "每页数量，最大100", defaultValue = "20")
        private long size = 20;

        @Size(max = 64)
        @Schema(description = "对账号，精确查询")
        private String reconciliationNo;

        @Size(max = 64)
        @Schema(description = "订单号，精确查询")
        private String orderNo;

        @Pattern(regexp = "ALIPAY|WECHAT")
        @Schema(description = "支付渠道", allowableValues = {"ALIPAY", "WECHAT"})
        private String channel;

        @Pattern(regexp = "MATCHED|REPAIRED|MISMATCH|CHANNEL_PENDING")
        @Schema(description = "对账结果", allowableValues = {"MATCHED", "REPAIRED", "MISMATCH", "CHANNEL_PENDING"})
        private String result;
    }
}
