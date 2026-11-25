package com.heima.infrastructure.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayResponse;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelPayment;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelRefund;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentInstruction;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlipayPaymentGateway implements PaymentChannelGateway {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AlipayPaymentProperties properties;

    @Override
    public String channel() {
        return "ALIPAY";
    }

    @Override
    public PaymentInstruction createPayment(PaymentCommand command) {
        requireConfigured(true);
        Map<String, Object> biz = paymentBiz(command);
        try {
            String body;
            if ("PAGE".equals(command.scene())) {
                biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
                AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                request.setNotifyUrl(properties.getNotifyUrl());
                request.setReturnUrl(properties.getReturnUrl());
                request.setBizContent(JSON.writeValueAsString(biz));
                body = client().pageExecute(request).getBody();
            } else if ("WAP".equals(command.scene())) {
                biz.put("product_code", "QUICK_WAP_WAY");
                AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                request.setNotifyUrl(properties.getNotifyUrl());
                request.setReturnUrl(properties.getReturnUrl());
                request.setBizContent(JSON.writeValueAsString(biz));
                body = client().pageExecute(request).getBody();
            } else {
                throw new ServiceException(400, "支付宝仅支持PAGE或WAP场景");
            }
            if (body == null || body.isBlank()) {
                throw new ServiceException(502, "支付宝未返回收银台表单");
            }
            return new PaymentInstruction(body, null, null);
        } catch (AlipayApiException e) {
            throw channelError("支付宝下单失败", e);
        }
    }

    @Override
    public ChannelPayment queryPayment(String orderNo) {
        requireConfigured(false);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(JSON.writeValueAsString(Map.of("out_trade_no", orderNo)));
        try {
            AlipayTradeQueryResponse response = client().execute(request);
            if (!response.isSuccess()) {
                if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                    return new ChannelPayment(orderNo, null, "NOT_EXIST", null, "CNY", properties.getAppId(), null, false);
                }
                throw responseError("支付宝查单失败", response);
            }
            requireSameOrder(orderNo, response.getOutTradeNo());
            String status = required(response.getTradeStatus(), "支付宝查单缺少交易状态");
            boolean paid = "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status);
            String currency = response.getTransCurrency() != null ? response.getTransCurrency()
                    : response.getPayCurrency() != null ? response.getPayCurrency() : "CNY";
            return new ChannelPayment(response.getOutTradeNo(), response.getTradeNo(), status,
                    decimal(response.getTotalAmount()), currency, properties.getAppId(), null, paid);
        } catch (AlipayApiException e) {
            throw channelError("支付宝查单失败", e);
        }
    }

    @Override
    public void closePayment(String orderNo) {
        requireConfigured(false);
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizContent(JSON.writeValueAsString(Map.of("out_trade_no", orderNo)));
        try {
            AlipayTradeCloseResponse response = client().execute(request);
            if (!response.isSuccess() && !"ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                throw responseError("支付宝关单失败", response);
            }
        } catch (AlipayApiException e) {
            throw channelError("支付宝关单失败", e);
        }
    }

    @Override
    public ChannelRefund createRefund(RefundCommand command) {
        requireConfigured(false);
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", command.orderNo());
        biz.put("refund_amount", money(command.amount()));
        biz.put("out_request_no", command.refundNo());
        biz.put("refund_reason", command.reason());
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizContent(JSON.writeValueAsString(biz));
        try {
            AlipayTradeRefundResponse response = client().execute(request);
            if (!response.isSuccess()) {
                throw responseError("支付宝退款失败", response);
            }
            requireSameOrder(command.orderNo(), response.getOutTradeNo());
            boolean succeeded = "Y".equals(response.getFundChange());
            BigDecimal refundAmount = decimal(response.getRefundFee());
            if (succeeded && (refundAmount == null || refundAmount.compareTo(command.amount()) != 0)) {
                throw new ServiceException(502, "支付宝退款金额与申请金额不一致");
            }
            return new ChannelRefund(command.refundNo(), response.getOutTradeNo(), response.getTradeNo(),
                    command.refundNo(), succeeded ? "REFUND_SUCCESS" : "PROCESSING", refundAmount, "CNY", succeeded);
        } catch (AlipayApiException e) {
            throw channelError("支付宝退款失败", e);
        }
    }

    @Override
    public ChannelRefund queryRefund(String refundNo, String orderNo) {
        requireConfigured(false);
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        request.setBizContent(JSON.writeValueAsString(Map.of("out_trade_no", orderNo, "out_request_no", refundNo)));
        try {
            AlipayTradeFastpayRefundQueryResponse response = client().execute(request);
            if (!response.isSuccess()) {
                if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode()) || "ACQ.REFUND_NOT_EXIST".equals(response.getSubCode())) {
                    return new ChannelRefund(refundNo, orderNo, null, refundNo, "NOT_EXIST", null, "CNY", false);
                }
                throw responseError("支付宝退款查询失败", response);
            }
            requireSameOrder(orderNo, response.getOutTradeNo());
            String status = required(response.getRefundStatus(), "支付宝退款查询缺少状态");
            return new ChannelRefund(refundNo, response.getOutTradeNo(), response.getTradeNo(), refundNo, status,
                    decimal(response.getRefundAmount()), "CNY", "REFUND_SUCCESS".equals(status));
        } catch (AlipayApiException e) {
            throw channelError("支付宝退款查询失败", e);
        }
    }

    @Override
    public PaymentNotification parsePaymentNotification(Map<String, String> parameters) {
        requireConfigured(false);
        Map<String, String> values = new TreeMap<>(parameters);
        try {
            if (!AlipaySignature.rsaCheckV2(new HashMap<>(values), properties.getAlipayPublicKey(), "UTF-8", "RSA2")) {
                throw new ServiceException(400, "支付宝通知验签失败");
            }
        } catch (AlipayApiException e) {
            throw new ServiceException(400, "支付宝通知验签失败", e);
        }
        String appId = required(values.get("app_id"), "支付宝通知缺少app_id");
        if (!properties.getAppId().equals(appId)) {
            throw new ServiceException(400, "支付宝通知app_id不匹配");
        }
        String orderNo = required(values.get("out_trade_no"), "支付宝通知缺少商户订单号");
        String status = required(values.get("trade_status"), "支付宝通知缺少交易状态");
        boolean success = "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status);
        String notificationId = values.get("notify_id");
        String raw = JSON.writeValueAsString(values);
        if (notificationId == null || notificationId.isBlank()) {
            notificationId = "ALI-" + UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
        }
        return new PaymentNotification(notificationId, orderNo, values.get("trade_no"), status,
                decimal(values.get("total_amount")), values.getOrDefault("currency", "CNY"), appId, null, success, raw);
    }

    private Map<String, Object> paymentBiz(PaymentCommand command) {
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", command.orderNo());
        biz.put("total_amount", money(command.amount()));
        biz.put("subject", command.subject());
        biz.put("time_expire", ALIPAY_TIME.format(command.expireAt()));
        return biz;
    }

    private DefaultAlipayClient client() {
        return new DefaultAlipayClient(properties.getGateway(), properties.getAppId(), properties.getPrivateKey(),
                "json", "UTF-8", properties.getAlipayPublicKey(), "RSA2");
    }

    private void requireConfigured(boolean creatingPayment) {
        if (!properties.isEnabled() || blank(properties.getAppId()) || blank(properties.getPrivateKey())
                || blank(properties.getAlipayPublicKey()) || blank(properties.getGateway())
                || creatingPayment && blank(properties.getNotifyUrl())) {
            throw new ServiceException(503, "支付宝支付未配置或已停用");
        }
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(400, message);
        }
        return value;
    }

    private static void requireSameOrder(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new ServiceException(502, "支付宝响应商户订单号不匹配");
        }
    }

    private static ServiceException responseError(String message, AlipayResponse response) {
        String detail = response.getSubCode() == null ? response.getCode() : response.getSubCode();
        return new ServiceException(502, message + "：" + detail);
    }

    private static ServiceException channelError(String message, Exception e) {
        return new ServiceException(502, message, e);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
