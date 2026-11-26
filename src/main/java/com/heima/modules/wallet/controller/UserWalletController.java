package com.heima.modules.wallet.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.wallet.dto.WalletDtos.PageQuery;
import com.heima.modules.wallet.dto.WalletDtos.WithdrawRequest;
import com.heima.modules.wallet.service.WalletService;
import com.heima.modules.wallet.vo.WalletVOs.AccountVO;
import com.heima.modules.wallet.vo.WalletVOs.LedgerVO;
import com.heima.modules.wallet.vo.WalletVOs.WithdrawalVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/wallet")
@RequiredArgsConstructor
@Tag(name = "用户返佣钱包")
public class UserWalletController {
    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "查询独立钱包可用、冻结和累计返佣余额")
    public R<AccountVO> account() {
        return R.ok(walletService.account(UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/ledgers")
    @Operation(summary = "分页查询不可变钱包流水")
    public R<PageResult<LedgerVO>> ledgers(@Valid @ModelAttribute PageQuery query) {
        return R.ok(walletService.ledgers(UserUtil.requireFrontUser().getId(), query));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "分页查询自己的提现记录")
    public R<PageResult<WithdrawalVO>> withdrawals(@Valid @ModelAttribute PageQuery query) {
        return R.ok(walletService.userWithdrawals(UserUtil.requireFrontUser().getId(), query));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "申请支付宝提现并冻结相应余额，等待管理员线下转账和人工确认")
    public R<WithdrawalVO> apply(@Valid @RequestBody WithdrawRequest request) {
        return R.ok(walletService.applyWithdrawal(UserUtil.requireFrontUser().getId(), request));
    }
}
