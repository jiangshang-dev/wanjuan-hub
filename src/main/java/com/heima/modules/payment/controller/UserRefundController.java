package com.heima.modules.payment.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.vo.OrderVO;
import com.heima.modules.payment.service.RefundProcessingService;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/refunds")
@RequiredArgsConstructor
@Tag(name = "用户退款")
public class UserRefundController {
    private final RefundProcessingService refundService;

    @PostMapping("/{orderNo}")
    @Operation(summary = "为已支付订单申请全额退款")
    public R<OrderVO.RefundVO> request(@PathVariable String orderNo, @Valid @RequestBody OrderDtos.RefundRequest request) {
        return R.ok(refundService.request(orderNo, request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping
    @Operation(summary = "分页查询自己的退款单")
    public R<PageResult<OrderVO.RefundVO>> page(@Valid @ModelAttribute OrderDtos.RefundQuery query) {
        return R.ok(refundService.userPage(query, UserUtil.requireFrontUser().getId()));
    }
}
