package com.heima.modules.wallet.service;

import com.heima.common.PageResult;
import com.heima.modules.wallet.dto.WalletDtos.HandleRequest;
import com.heima.modules.wallet.dto.WalletDtos.PageQuery;
import com.heima.modules.wallet.dto.WalletDtos.WithdrawRequest;
import com.heima.modules.wallet.vo.WalletVOs.AccountVO;
import com.heima.modules.wallet.vo.WalletVOs.LedgerVO;
import com.heima.modules.wallet.vo.WalletVOs.WithdrawalVO;

import java.math.BigDecimal;

public interface WalletService {
    void credit(Long userId, String bizType, String bizId, BigDecimal amount, String remark);
    AccountVO account(Long userId);
    PageResult<LedgerVO> ledgers(Long userId, PageQuery query);
    WithdrawalVO applyWithdrawal(Long userId, WithdrawRequest request);
    PageResult<WithdrawalVO> userWithdrawals(Long userId, PageQuery query);
    PageResult<WithdrawalVO> adminWithdrawals(PageQuery query);
    WithdrawalVO adminWithdrawal(Long id);
    WithdrawalVO handleWithdrawal(Long id, HandleRequest request, Long adminId);
}
