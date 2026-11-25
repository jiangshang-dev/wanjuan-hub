package com.heima.modules.payment.controller;

import com.heima.common.R;
import com.heima.modules.order.vo.OrderVO;
import com.heima.modules.payment.dto.PaymentDtos;
import com.heima.modules.payment.service.PaymentProcessingService;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/payments")
@RequiredArgsConstructor
@Tag(name = "用户支付")
public class UserPaymentController {
    private final PaymentProcessingService paymentService;

    @PostMapping("/{orderNo}")
    @Operation(summary = "创建真实支付宝或微信支付单")
    public R<OrderVO.PaymentVO> pay(@PathVariable String orderNo, @Valid @RequestBody PaymentDtos.PayRequest request,
                                    HttpServletRequest servletRequest) {
        return R.ok(paymentService.pay(orderNo, request, UserUtil.requireFrontUser().getId(), servletRequest.getRemoteAddr()));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查看自己的支付单及收银台信息")
    public R<OrderVO.PaymentVO> detail(@PathVariable String orderNo) {
        return R.ok(paymentService.userPayment(orderNo, UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/{orderNo}/close")
    @Operation(summary = "主动查单后关闭待支付订单")
    public R<OrderVO> close(@PathVariable String orderNo) {
        return R.ok(paymentService.close(orderNo, UserUtil.requireFrontUser().getId(), "用户取消"));
    }
}
