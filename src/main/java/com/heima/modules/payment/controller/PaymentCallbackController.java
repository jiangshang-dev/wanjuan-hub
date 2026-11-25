package com.heima.modules.payment.controller;

import com.heima.infrastructure.payment.AlipayPaymentGateway;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundNotification;
import com.heima.infrastructure.payment.WechatPaymentGateway;
import com.heima.modules.payment.service.PaymentProcessingService;
import com.heima.modules.payment.service.RefundProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments/callback")
@RequiredArgsConstructor
@Tag(name = "支付渠道异步通知")
public class PaymentCallbackController {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AlipayPaymentGateway alipayGateway;
    private final WechatPaymentGateway wechatGateway;
    private final PaymentProcessingService paymentService;
    private final RefundProcessingService refundService;

    @PostMapping("/alipay")
    @Operation(summary = "支付宝异步通知；验签并处理成功后返回success")
    public String alipay(@RequestParam Map<String, String> parameters) {
        try {
            PaymentNotification notification = alipayGateway.parsePaymentNotification(parameters);
            paymentService.handleNotification("ALIPAY", notification);
            return "success";
        } catch (DuplicateKeyException e) {
            return "success";
        } catch (RuntimeException e) {
            return "failure";
        }
    }

    @PostMapping("/wechat")
    @Operation(summary = "微信支付及退款API v3异步通知")
    public ResponseEntity<Map<String, String>> wechat(@RequestBody String body, HttpServletRequest request) {
        try {
            String eventType = JSON.readTree(body).path("event_type").asText();
            Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                    .collect(Collectors.toMap(Function.identity(), request::getHeader, (left, right) -> left));
            if (eventType != null && eventType.startsWith("REFUND.")) {
                RefundNotification notification = wechatGateway.parseRefundNotification(body, headers);
                refundService.handleNotification("WECHAT", notification);
            } else {
                PaymentNotification notification = wechatGateway.parsePaymentNotification(body, headers);
                paymentService.handleNotification("WECHAT", notification);
            }
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "FAIL", "message", "处理失败"));
        }
    }
}
