package com.heima.infrastructure.payment;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelPayment;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelRefund;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentInstruction;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundNotification;
import com.wechat.pay.java.core.RSAConfig;
import com.wechat.pay.java.core.notification.Notification;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RSANotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.H5Info;
import com.wechat.pay.java.service.payments.h5.model.SceneInfo;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WechatPaymentGateway implements PaymentChannelGateway {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final WechatPaymentProperties properties;

    @Override
    public String channel() {
        return "WECHAT";
    }

    @Override
    public PaymentInstruction createPayment(PaymentCommand command) {
        requireConfigured(true);
        try {
            if ("NATIVE".equals(command.scene())) {
                com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest request =
                        new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
                request.setAppid(properties.getAppId());
                request.setMchid(properties.getMchId());
                request.setDescription(command.subject());
                request.setOutTradeNo(command.orderNo());
                request.setNotifyUrl(properties.getNotifyUrl());
                request.setTimeExpire(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(command.expireAt().atOffset(ZoneOffset.ofHours(8))));
                com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
                        new com.wechat.pay.java.service.payments.nativepay.model.Amount();
                amount.setTotal(centsInt(command.amount()));
                amount.setCurrency("CNY");
                request.setAmount(amount);
                String codeUrl = nativeService().prepay(request).getCodeUrl();
                if (blank(codeUrl)) {
                    throw new ServiceException(502, "微信支付未返回Native二维码内容");
                }
                return new PaymentInstruction(null, codeUrl, null);
            }
            if ("H5".equals(command.scene())) {
                com.wechat.pay.java.service.payments.h5.model.PrepayRequest request =
                        new com.wechat.pay.java.service.payments.h5.model.PrepayRequest();
                request.setAppid(properties.getAppId());
                request.setMchid(properties.getMchId());
                request.setDescription(command.subject());
                request.setOutTradeNo(command.orderNo());
                request.setNotifyUrl(properties.getNotifyUrl());
                request.setTimeExpire(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(command.expireAt().atOffset(ZoneOffset.ofHours(8))));
                com.wechat.pay.java.service.payments.h5.model.Amount amount =
                        new com.wechat.pay.java.service.payments.h5.model.Amount();
                amount.setTotal(centsInt(command.amount()));
                amount.setCurrency("CNY");
                request.setAmount(amount);
                SceneInfo sceneInfo = new SceneInfo();
                sceneInfo.setPayerClientIp(command.clientIp());
                H5Info h5Info = new H5Info();
                h5Info.setType("WAP");
                sceneInfo.setH5Info(h5Info);
                request.setSceneInfo(sceneInfo);
                String h5Url = h5Service().prepay(request).getH5Url();
                if (blank(h5Url)) {
                    throw new ServiceException(502, "微信支付未返回H5支付链接");
                }
                return new PaymentInstruction(null, null, h5Url);
            }
            throw new ServiceException(400, "微信支付仅支持NATIVE或H5场景");
        } catch (ServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException(502, "微信支付下单失败", e);
        }
    }

    @Override
    public ChannelPayment queryPayment(String orderNo) {
        requireConfigured(false);
        try {
            Transaction transaction;
            // 查单不依赖本地场景，Native与H5共用同一交易查询接口。
            com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest request =
                    new com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest();
            request.setMchid(properties.getMchId());
            request.setOutTradeNo(orderNo);
            transaction = nativeService().queryOrderByOutTradeNo(request);
            return channelPayment(transaction, orderNo);
        } catch (ServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException(502, "微信支付查单失败", e);
        }
    }

    @Override
    public void closePayment(String orderNo) {
        requireConfigured(false);
        try {
            com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest request =
                    new com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest();
            request.setMchid(properties.getMchId());
            request.setOutTradeNo(orderNo);
            nativeService().closeOrder(request);
        } catch (RuntimeException e) {
            throw new ServiceException(502, "微信支付关单失败", e);
        }
    }

    @Override
    public ChannelRefund createRefund(RefundCommand command) {
        requireConfigured(false);
        try {
            CreateRequest request = new CreateRequest();
            request.setOutTradeNo(command.orderNo());
            request.setTransactionId(command.channelTradeNo());
            request.setOutRefundNo(command.refundNo());
            request.setReason(command.reason());
            request.setNotifyUrl(properties.getNotifyUrl());
            AmountReq amount = new AmountReq();
            amount.setRefund(centsLong(command.amount()));
            amount.setTotal(centsLong(command.totalAmount()));
            amount.setCurrency("CNY");
            request.setAmount(amount);
            return channelRefund(refundService().create(request), command.refundNo(), command.orderNo());
        } catch (ServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException(502, "微信支付退款失败", e);
        }
    }

    @Override
    public ChannelRefund queryRefund(String refundNo, String orderNo) {
        requireConfigured(false);
        try {
            QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
            request.setOutRefundNo(refundNo);
            return channelRefund(refundService().queryByOutRefundNo(request), refundNo, orderNo);
        } catch (ServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServiceException(502, "微信支付退款查询失败", e);
        }
    }

    @Override
    public PaymentNotification parsePaymentNotification(String body, Map<String, String> headers) {
        requireConfigured(false);
        Notification notification = parseNotification(body, headers);
        Transaction transaction = JSON.readValue(notification.getPlaintext(), Transaction.class);
        ChannelPayment payment = channelPayment(transaction, transaction.getOutTradeNo());
        boolean success = "TRANSACTION.SUCCESS".equals(notification.getEventType()) && "SUCCESS".equals(payment.status());
        return new PaymentNotification(notification.getId(), payment.orderNo(), payment.channelTradeNo(),
                notification.getEventType(), payment.amount(), payment.currency(), payment.appId(), payment.mchId(), success, body);
    }

    @Override
    public RefundNotification parseRefundNotification(String body, Map<String, String> headers) {
        requireConfigured(false);
        Notification notification = parseNotification(body, headers);
        com.wechat.pay.java.service.refund.model.RefundNotification refund = JSON.readValue(
                notification.getPlaintext(), com.wechat.pay.java.service.refund.model.RefundNotification.class);
        requireSame(refund.getOutRefundNo(), "微信退款通知缺少商户退款单号");
        requireSame(refund.getOutTradeNo(), "微信退款通知缺少商户订单号");
        String status = refund.getRefundStatus() == null ? null : refund.getRefundStatus().name();
        boolean success = "REFUND.SUCCESS".equals(notification.getEventType()) && "SUCCESS".equals(status);
        BigDecimal amount = refund.getAmount() == null || refund.getAmount().getRefund() == null
                ? null : BigDecimal.valueOf(refund.getAmount().getRefund(), 2);
        String currency = refund.getAmount() == null ? null : refund.getAmount().getCurrency();
        if (success && (amount == null || amount.signum() <= 0 || !"CNY".equals(currency))) {
            throw new ServiceException(400, "微信退款通知金额或币种无效");
        }
        return new PaymentGatewayModels.RefundNotification(notification.getId(), refund.getOutRefundNo(), refund.getOutTradeNo(),
                refund.getTransactionId(), refund.getRefundId(), notification.getEventType(), status, amount, currency, success, body);
    }

    private Notification parseNotification(String body, Map<String, String> headers) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(header(headers, "Wechatpay-Serial"))
                    .timestamp(header(headers, "Wechatpay-Timestamp"))
                    .nonce(header(headers, "Wechatpay-Nonce"))
                    .signature(header(headers, "Wechatpay-Signature"))
                    .signType("WECHATPAY2-SHA256-RSA2048")
                    .body(body).build();
            RSANotificationConfig config = new RSANotificationConfig.Builder()
                    .certificatesFromPath(properties.getPlatformCertificatePath())
                    .apiV3Key(properties.getApiV3Key()).build();
            return new NotificationParser(config).parse(requestParam, Notification.class);
        } catch (RuntimeException e) {
            throw new ServiceException(400, "微信支付通知验签或解密失败", e);
        }
    }

    private ChannelPayment channelPayment(Transaction transaction, String expectedOrderNo) {
        if (transaction == null) {
            throw new ServiceException(502, "微信支付未返回交易信息");
        }
        if (expectedOrderNo != null && !expectedOrderNo.equals(transaction.getOutTradeNo())) {
            throw new ServiceException(502, "微信支付响应商户订单号不匹配");
        }
        if (!properties.getAppId().equals(transaction.getAppid()) || !properties.getMchId().equals(transaction.getMchid())) {
            throw new ServiceException(400, "微信支付appid或mchid不匹配");
        }
        String status = transaction.getTradeState() == null ? null : transaction.getTradeState().name();
        boolean paid = "SUCCESS".equals(status);
        BigDecimal amount = transaction.getAmount() == null || transaction.getAmount().getTotal() == null
                ? null : BigDecimal.valueOf(transaction.getAmount().getTotal(), 2);
        String currency = transaction.getAmount() == null ? null : transaction.getAmount().getCurrency();
        if (paid && (amount == null || amount.signum() <= 0 || !"CNY".equals(currency))) {
            throw new ServiceException(400, "微信支付金额或币种无效");
        }
        return new ChannelPayment(transaction.getOutTradeNo(), transaction.getTransactionId(), status, amount,
                currency, transaction.getAppid(), transaction.getMchid(), paid);
    }

    private ChannelRefund channelRefund(Refund refund, String expectedRefundNo, String expectedOrderNo) {
        if (refund == null || !expectedRefundNo.equals(refund.getOutRefundNo())) {
            throw new ServiceException(502, "微信支付退款响应商户退款单号不匹配");
        }
        if (expectedOrderNo != null && !expectedOrderNo.equals(refund.getOutTradeNo())) {
            throw new ServiceException(502, "微信支付退款响应商户订单号不匹配");
        }
        String status = refund.getStatus() == null ? null : refund.getStatus().name();
        boolean success = "SUCCESS".equals(status);
        BigDecimal amount = refund.getAmount() == null || refund.getAmount().getRefund() == null
                ? null : BigDecimal.valueOf(refund.getAmount().getRefund(), 2);
        String currency = refund.getAmount() == null ? null : refund.getAmount().getCurrency();
        if (success && (amount == null || amount.signum() <= 0 || !"CNY".equals(currency) || blank(refund.getRefundId()))) {
            throw new ServiceException(502, "微信支付退款成功响应缺少金额、币种或退款号");
        }
        return new ChannelRefund(refund.getOutRefundNo(), refund.getOutTradeNo(), refund.getTransactionId(),
                refund.getRefundId(), status, amount, currency, success);
    }

    private RSAConfig config() {
        return new RSAConfig.Builder().merchantId(properties.getMchId())
                .privateKeyFromPath(properties.getPrivateKeyPath())
                .merchantSerialNumber(properties.getMerchantSerialNumber())
                .wechatPayCertificatesFromPath(properties.getPlatformCertificatePath()).build();
    }

    private NativePayService nativeService() {
        return new NativePayService.Builder().config(config()).build();
    }

    private H5Service h5Service() {
        return new H5Service.Builder().config(config()).build();
    }

    private RefundService refundService() {
        return new RefundService.Builder().config(config()).build();
    }

    private void requireConfigured(boolean creatingPayment) {
        if (!properties.isEnabled() || blank(properties.getAppId()) || blank(properties.getMchId())
                || blank(properties.getApiV3Key()) || blank(properties.getMerchantSerialNumber())
                || blank(properties.getPrivateKeyPath()) || blank(properties.getPlatformCertificatePath())
                || creatingPayment && blank(properties.getNotifyUrl())) {
            throw new ServiceException(503, "微信支付未配置或已停用");
        }
    }

    private static int centsInt(BigDecimal amount) {
        return amount.movePointRight(2).intValueExact();
    }

    private static long centsLong(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private static String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private static String requireSame(String value, String message) {
        if (blank(value)) {
            throw new ServiceException(400, message);
        }
        return value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
