package com.heima.modules.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "资源购买订单")
public class OrderVO {
    @Schema(description = "订单ID") private Long id;
    @Schema(description = "订单号") private String orderNo;
    @Schema(description = "购买用户ID") private Long userId;
    @Schema(description = "订单类型：RESOURCE历史资源订单，MEMBERSHIP会员订单") private String orderType;
    @Schema(description = "资源ID，会员订单为空") private Long resourceId;
    @Schema(description = "会员套餐ID，资源订单为空") private Long membershipPlanId;
    @Schema(description = "下单时套餐有效天数快照") private Integer membershipDurationDays;
    @Schema(description = "下单时套餐每日领取数量快照") private Integer membershipDailyLimit;
    @Schema(description = "订单金额") private BigDecimal totalAmount;
    @Schema(description = "币种") private String currency;
    @Schema(description = "客户端类型") private String clientType;
    @Schema(description = "订单状态") private String status;
    @Schema(description = "资源授权状态") private String grantStatus;
    @Schema(description = "支付截止时间") private LocalDateTime expireAt;
    @Schema(description = "支付完成时间") private LocalDateTime paidAt;
    @Schema(description = "关闭时间") private LocalDateTime closedAt;
    @Schema(description = "关闭原因") private String closeReason;
    @Schema(description = "创建时间") private LocalDateTime createTime;
    @Schema(description = "订单明细") private List<ItemVO> items;
    @Schema(description = "支付单") private PaymentVO payment;
    @Schema(description = "退款单") private RefundVO refund;

    @Data
    @Schema(description = "订单资源明细")
    public static class ItemVO {
        @Schema(description = "资源ID") private Long resourceId;
        @Schema(description = "下单时资源名称") private String resourceName;
        @Schema(description = "下单时资源价格") private BigDecimal price;
        @Schema(description = "资源所有者ID") private Long ownerId;
    }

    @Data
    @Schema(description = "支付单")
    public static class PaymentVO {
        @Schema(description = "支付单号") private String paymentNo;
        @Schema(description = "支付渠道") private String channel;
        @Schema(description = "支付场景") private String scene;
        @Schema(description = "支付金额") private BigDecimal amount;
        @Schema(description = "币种") private String currency;
        @Schema(description = "支付状态") private String status;
        @Schema(description = "渠道交易号") private String channelTradeNo;
        @Schema(description = "支付宝收银台表单") private String formHtml;
        @Schema(description = "微信Native二维码内容") private String codeUrl;
        @Schema(description = "微信H5支付链接") private String h5Url;
        @Schema(description = "渠道状态") private String channelStatus;
        @Schema(description = "支付完成时间") private LocalDateTime paidAt;
        @Schema(description = "最近主动查单时间") private LocalDateTime lastQueryAt;
        @Schema(description = "渠道错误信息") private String errorMessage;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data
    @Schema(description = "退款单")
    public static class RefundVO {
        @Schema(description = "退款单ID") private Long id;
        @Schema(description = "退款单号") private String refundNo;
        @Schema(description = "订单号") private String orderNo;
        @Schema(description = "申请用户ID") private Long userId;
        @Schema(description = "支付渠道") private String channel;
        @Schema(description = "退款金额") private BigDecimal amount;
        @Schema(description = "币种") private String currency;
        @Schema(description = "退款原因") private String reason;
        @Schema(description = "退款状态") private String status;
        @Schema(description = "渠道退款号") private String channelRefundNo;
        @Schema(description = "渠道退款状态") private String channelStatus;
        @Schema(description = "审核意见") private String reviewOpinion;
        @Schema(description = "审核时间") private LocalDateTime reviewedAt;
        @Schema(description = "退款成功时间") private LocalDateTime refundedAt;
        @Schema(description = "是否已撤销授权") private Boolean entitlementRevoked;
        @Schema(description = "渠道错误信息") private String errorMessage;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data
    @Schema(description = "支付对账记录")
    public static class ReconciliationVO {
        @Schema(description = "对账号") private String reconciliationNo;
        @Schema(description = "订单号") private String orderNo;
        @Schema(description = "支付渠道") private String channel;
        @Schema(description = "触发方式") private String triggeredBy;
        @Schema(description = "对账结果") private String result;
        @Schema(description = "本地订单金额") private BigDecimal expectedAmount;
        @Schema(description = "渠道支付金额") private BigDecimal channelAmount;
        @Schema(description = "本地订单状态") private String expectedStatus;
        @Schema(description = "渠道支付状态") private String channelStatus;
        @Schema(description = "渠道交易号") private String channelTradeNo;
        @Schema(description = "对账说明") private String detail;
        @Schema(description = "对账时间") private LocalDateTime reconciledAt;
    }
}
