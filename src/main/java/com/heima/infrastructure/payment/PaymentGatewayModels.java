package com.heima.infrastructure.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentGatewayModels {
    private PaymentGatewayModels() {
    }

    public record PaymentCommand(String paymentNo, String orderNo, String subject, BigDecimal amount,
                                 String scene, String clientIp, LocalDateTime expireAt) {
    }

    public record PaymentInstruction(String formHtml, String codeUrl, String h5Url) {
    }

    public record ChannelPayment(String orderNo, String channelTradeNo, String status, BigDecimal amount,
                                 String currency, String appId, String mchId, boolean paid) {
    }

    public record PaymentNotification(String notificationId, String orderNo, String channelTradeNo,
                                      String eventType, BigDecimal amount, String currency, String appId,
                                      String mchId, boolean success, String rawPayload) {
    }

    public record RefundCommand(String refundNo, String orderNo, String channelTradeNo, BigDecimal amount,
                                BigDecimal totalAmount, String reason) {
    }

    public record ChannelRefund(String refundNo, String orderNo, String channelTradeNo, String channelRefundNo,
                                String status, BigDecimal amount, String currency, boolean succeeded) {
    }

    public record RefundNotification(String notificationId, String refundNo, String orderNo, String channelTradeNo,
                                     String channelRefundNo, String eventType, String status, BigDecimal amount,
                                     String currency, boolean success, String rawPayload) {
    }
}
