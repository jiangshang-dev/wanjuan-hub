package com.heima.modules.order.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.service.OrderService;
import com.heima.modules.order.vo.OrderVO;
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
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
@Tag(name = "用户资源订单")
public class UserOrderController {
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "按资源当前价格创建购买订单")
    public R<OrderVO> create(@Valid @RequestBody OrderDtos.CreateRequest request) {
        return R.ok(orderService.create(request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping
    @Operation(summary = "分页查询自己的订单")
    public R<PageResult<OrderVO>> page(@Valid @ModelAttribute OrderDtos.Query query) {
        return R.ok(orderService.userPage(query, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查看自己的订单、支付及退款详情")
    public R<OrderVO> detail(@PathVariable String orderNo) {
        return R.ok(orderService.userDetail(orderNo, UserUtil.requireFrontUser().getId()));
    }
}
