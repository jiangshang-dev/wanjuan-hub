package com.heima.infrastructure.payment;

import com.heima.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayRegistry {
    private final Map<String, PaymentChannelGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentChannelGateway> gateways) {
        this.gateways = gateways.stream().collect(Collectors.toUnmodifiableMap(PaymentChannelGateway::channel, Function.identity()));
    }

    public PaymentChannelGateway get(String channel) {
        PaymentChannelGateway gateway = gateways.get(channel);
        if (gateway == null) {
            throw new ServiceException(400, "不支持的支付渠道");
        }
        return gateway;
    }
}
