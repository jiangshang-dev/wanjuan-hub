package com.heima.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    @Data
    @Schema(description = "发起支付")
    public static class PayRequest {
        @NotBlank
        @Pattern(regexp = "ALIPAY|WECHAT")
        @Schema(description = "支付渠道", allowableValues = {"ALIPAY", "WECHAT"})
        private String channel;

        @NotBlank
        @Pattern(regexp = "PAGE|WAP|NATIVE|H5")
        @Schema(description = "支付场景：支付宝PAGE/WAP，微信NATIVE/H5", allowableValues = {"PAGE", "WAP", "NATIVE", "H5"})
        private String scene;
    }
}
