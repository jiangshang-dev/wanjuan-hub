package com.heima.modules.payment;

import com.alipay.api.internal.util.AlipaySignature;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.AlipayPaymentGateway;
import com.heima.infrastructure.payment.AlipayPaymentProperties;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlipayPaymentGatewayTest {
    private AlipayPaymentGateway gateway;
    private String privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        AlipayPaymentProperties properties = new AlipayPaymentProperties();
        properties.setEnabled(true);
        properties.setAppId("expected-app");
        properties.setPrivateKey("not-used-by-notification-parser");
        properties.setAlipayPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        gateway = new AlipayPaymentGateway(properties);
    }

    @Test
    void validSignatureAndAppAreAccepted() throws Exception {
        Map<String, String> notification = signedNotification("expected-app");

        PaymentNotification parsed = gateway.parsePaymentNotification(notification);

        assertTrue(parsed.success());
        assertEquals("O1", parsed.orderNo());
        assertEquals("12.34", parsed.amount().toPlainString());
    }

    @Test
    void wrongAppIsRejectedEvenWithValidSignature() throws Exception {
        Map<String, String> notification = signedNotification("other-app");

        ServiceException error = assertThrows(ServiceException.class, () -> gateway.parsePaymentNotification(notification));

        assertEquals(400, error.getCode());
    }

    @Test
    void tamperedAmountFailsSignatureVerification() throws Exception {
        Map<String, String> notification = signedNotification("expected-app");
        notification.put("total_amount", "0.01");

        ServiceException error = assertThrows(ServiceException.class, () -> gateway.parsePaymentNotification(notification));

        assertEquals(400, error.getCode());
    }

    private Map<String, String> signedNotification(String appId) throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("notify_id", "N1");
        values.put("app_id", appId);
        values.put("out_trade_no", "O1");
        values.put("trade_no", "T1");
        values.put("trade_status", "TRADE_SUCCESS");
        values.put("total_amount", "12.34");
        values.put("currency", "CNY");
        values.put("sign_type", "RSA2");
        String content = AlipaySignature.getSignCheckContentV2(values);
        values.put("sign", AlipaySignature.rsa256Sign(content, privateKey, "UTF-8"));
        return values;
    }
}
