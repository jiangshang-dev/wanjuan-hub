package com.heima.modules.order.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.service.OrderService;
import com.heima.modules.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "后台订单管理")
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "分页查询全部订单")
    public R<PageResult<OrderVO>> page(@Valid @ModelAttribute OrderDtos.Query query) {
        return R.ok(orderService.adminPage(query));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查看订单、支付及退款详情")
    public R<OrderVO> detail(@PathVariable String orderNo) {
        return R.ok(orderService.adminDetail(orderNo));
    }
}
