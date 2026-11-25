package com.heima.infrastructure.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wanjuan.payment.alipay")
public class AlipayPaymentProperties {
    private boolean enabled;
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gateway = "https://openapi.alipay.com/gateway.do";
    private String notifyUrl;
    private String returnUrl;
}
