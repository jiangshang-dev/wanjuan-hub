package com.heima.modules.membership.controller;

import com.heima.common.R;
import com.heima.modules.membership.dto.MembershipDtos.CreateOrderRequest;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.membership.vo.MembershipVOs.StatusVO;
import com.heima.modules.order.service.OrderService;
import com.heima.modules.order.vo.OrderVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/paid-membership")
@RequiredArgsConstructor
@Tag(name = "用户付费会员")
public class UserMembershipController {
    private final MembershipService membershipService;
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "查询我的会员有效期及今日领取额度")
    public R<StatusVO> status() {
        return R.ok(membershipService.status(UserUtil.requireFrontUser().getId()));
    }

    @PostMapping("/orders")
    @Operation(summary = "创建会员购买订单；仅真实支付通知确认成功后开通或续费")
    public R<OrderVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return R.ok(orderService.createMembership(request, UserUtil.requireFrontUser().getId()));
    }
}
