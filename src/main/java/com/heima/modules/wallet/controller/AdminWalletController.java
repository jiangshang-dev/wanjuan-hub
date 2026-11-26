package com.heima.modules.wallet.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.wallet.dto.WalletDtos.HandleRequest;
import com.heima.modules.wallet.dto.WalletDtos.PageQuery;
import com.heima.modules.wallet.service.WalletService;
import com.heima.modules.wallet.vo.WalletVOs.WithdrawalVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/wallet/withdrawals")
@RequiredArgsConstructor
@Tag(name = "后台人工提现管理")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {
    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "分页查询提现单，支付宝账号仅返回脱敏值")
    @PreAuthorize("hasAuthority('wallet:withdrawal:list')")
    public R<PageResult<WithdrawalVO>> page(@Valid @ModelAttribute PageQuery query) {
        return R.ok(walletService.adminWithdrawals(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询提现详情及完整支付宝账号，仅供运营人员线下转账使用")
    @PreAuthorize("hasAuthority('wallet:withdrawal:list')")
    public R<WithdrawalVO> detail(@PathVariable Long id) {
        return R.ok(walletService.adminWithdrawal(id));
    }

    @PutMapping("/{id}/handle")
    @Operation(summary = "人工确认已线下转账成功或拒绝解冻；系统不调用也不模拟第三方打款")
    @OperationLog(module = "钱包管理", value = "人工处理提现单", targetId = "#id")
    @PreAuthorize("hasAuthority('wallet:withdrawal:handle')")
    public R<WithdrawalVO> handle(@PathVariable Long id, @Valid @RequestBody HandleRequest request) {
        return R.ok(walletService.handleWithdrawal(id, request, UserUtil.requireAdmin().getId()));
    }
}
