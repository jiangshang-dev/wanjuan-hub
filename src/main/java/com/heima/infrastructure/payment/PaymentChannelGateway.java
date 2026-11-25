package com.heima.infrastructure.payment;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelPayment;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelRefund;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentInstruction;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundNotification;

import java.util.Map;

public interface PaymentChannelGateway {
    String channel();

    PaymentInstruction createPayment(PaymentCommand command);

    ChannelPayment queryPayment(String orderNo);

    void closePayment(String orderNo);

    ChannelRefund createRefund(RefundCommand command);

    ChannelRefund queryRefund(String refundNo, String orderNo);

    default PaymentNotification parsePaymentNotification(Map<String, String> parameters) {
        throw new ServiceException(400, "支付渠道通知格式不支持");
    }

    default PaymentNotification parsePaymentNotification(String body, Map<String, String> headers) {
        throw new ServiceException(400, "支付渠道通知格式不支持");
    }

    default RefundNotification parseRefundNotification(String body, Map<String, String> headers) {
        throw new ServiceException(400, "退款渠道通知格式不支持");
    }
}
