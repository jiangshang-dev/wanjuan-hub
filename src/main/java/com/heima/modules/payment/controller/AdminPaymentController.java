package com.heima.modules.payment.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.vo.OrderVO;
import com.heima.modules.payment.dto.PaymentAdminDtos;
import com.heima.modules.payment.service.PaymentAdminQueryService;
import com.heima.modules.payment.service.PaymentProcessingService;
import com.heima.modules.payment.service.RefundProcessingService;
import com.heima.modules.payment.vo.PaymentAdminVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台支付退款与对账")
public class AdminPaymentController {
    private final PaymentProcessingService paymentService;
    private final RefundProcessingService refundService;
    private final PaymentAdminQueryService paymentQueryService;

    @PostMapping("/payments/{orderNo}/reconcile")
    @Operation(summary = "主动查询渠道支付结果并生成对账记录")
    @OperationLog(module = "支付管理", value = "手动对账", targetId = "#orderNo")
    @PreAuthorize("hasAuthority('payment:reconcile')")
    public R<OrderVO.ReconciliationVO> reconcile(@PathVariable String orderNo) {
        return R.ok(paymentService.reconcile(orderNo, "MANUAL", UserUtil.requireAdmin().getId()));
    }

    @GetMapping("/refunds")
    @Operation(summary = "分页查询退款单")
    @PreAuthorize("hasAuthority('refund:list')")
    public R<PageResult<OrderVO.RefundVO>> refunds(@Valid @ModelAttribute OrderDtos.RefundQuery query) {
        return R.ok(refundService.adminPage(query));
    }

    @PutMapping("/refunds/{refundId}/review")
    @Operation(summary = "审核退款；同意后调用真实渠道退款")
    @OperationLog(module = "退款管理", value = "审核退款", targetId = "#refundId")
    @PreAuthorize("hasAuthority('refund:review')")
    public R<OrderVO.RefundVO> review(@PathVariable Long refundId, @Valid @RequestBody OrderDtos.RefundReviewRequest request) {
        return R.ok(refundService.review(refundId, request, UserUtil.requireAdmin().getId()));
    }

    @PostMapping("/refunds/{refundNo}/sync")
    @Operation(summary = "查询渠道退款进度，成功后撤销资源授权")
    @OperationLog(module = "退款管理", value = "同步渠道退款进度", targetId = "#refundNo")
    @PreAuthorize("hasAuthority('refund:sync')")
    public R<OrderVO.RefundVO> syncRefund(@PathVariable String refundNo) {
        return R.ok(refundService.synchronize(refundNo));
    }
}
