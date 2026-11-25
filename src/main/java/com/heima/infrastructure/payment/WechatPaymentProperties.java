package com.heima.infrastructure.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wanjuan.payment.wechat")
public class WechatPaymentProperties {
    private boolean enabled;
    private String appId;
    private String mchId;
    private String apiV3Key;
    private String merchantSerialNumber;
    private String privateKeyPath;
    private String platformCertificatePath;
    private String notifyUrl;
}
