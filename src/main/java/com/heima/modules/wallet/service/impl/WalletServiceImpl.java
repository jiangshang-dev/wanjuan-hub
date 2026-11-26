package com.heima.modules.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.resource.service.ResourceCryptoService;
import com.heima.modules.wallet.dto.WalletDtos.HandleRequest;
import com.heima.modules.wallet.dto.WalletDtos.PageQuery;
import com.heima.modules.wallet.dto.WalletDtos.WithdrawRequest;
import com.heima.modules.wallet.entity.WalletEntities.Account;
import com.heima.modules.wallet.entity.WalletEntities.Ledger;
import com.heima.modules.wallet.entity.WalletEntities.Withdrawal;
import com.heima.modules.wallet.mapper.WalletMappers.AccountMapper;
import com.heima.modules.wallet.mapper.WalletMappers.LedgerMapper;
import com.heima.modules.wallet.mapper.WalletMappers.WithdrawalMapper;
import com.heima.modules.wallet.service.WalletService;
import com.heima.modules.wallet.vo.WalletVOs.AccountVO;
import com.heima.modules.wallet.vo.WalletVOs.LedgerVO;
import com.heima.modules.wallet.vo.WalletVOs.WithdrawalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
    private final AccountMapper accountMapper;
    private final LedgerMapper ledgerMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final ResourceCryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void credit(Long userId, String bizType, String bizId, BigDecimal amount, String remark) {
        if (userId == null || amount == null || amount.signum() == 0) return;
        BigDecimal normalized = amount.setScale(4, RoundingMode.UNNECESSARY);
        if (normalized.signum() < 0) throw new ServiceException(500, "返佣金额不能为负数");
        if (ledgerMapper.selectByBiz(bizType, bizId) != null) return;
        Account account = lockAccount(userId);
        if (ledgerMapper.selectByBiz(bizType, bizId) != null) return;
        BigDecimal availableAfter = account.getAvailableBalance().add(normalized);
        account.setAvailableBalance(availableAfter);
        account.setTotalIncome(account.getTotalIncome().add(normalized));
        account.setVersion(account.getVersion() + 1);
        accountMapper.updateById(account);
        insertLedger(userId, bizType, bizId, normalized, BigDecimal.ZERO, availableAfter,
                account.getFrozenBalance(), remark);
        eventPublisher.publishEvent(new UserNotificationEvent(userId, "PAYMENT", "返佣到账",
                (remark == null || remark.isBlank() ? "资源返佣已到账" : remark)
                        + "，到账金额：" + normalized.stripTrailingZeros().toPlainString() + "元",
                bizType, bizId, "WALLET_CREDIT:" + bizType + ":" + bizId));
    }

    @Override
    @Transactional
    public AccountVO account(Long userId) {
        accountMapper.createIgnore(userId);
        Account account = accountMapper.selectById(userId);
        AccountVO vo = new AccountVO();
        BeanUtils.copyProperties(account, vo);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LedgerVO> ledgers(Long userId, PageQuery query) {
        Page<Ledger> result = ledgerMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Ledger>().eq(Ledger::getUserId, userId).orderByDesc(Ledger::getId));
        return PageResult.of(result.getRecords().stream().map(item -> {
            LedgerVO vo = new LedgerVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    @Transactional
    public WithdrawalVO applyWithdrawal(Long userId, WithdrawRequest request) {
        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.UNNECESSARY);
        Account account = lockAccount(userId);
        if (account.getAvailableBalance().compareTo(amount) < 0) throw new ServiceException(409, "可提现余额不足");
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWithdrawalNo("W" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        withdrawal.setUserId(userId);
        withdrawal.setAmount(amount);
        withdrawal.setAlipayAccount(cryptoService.encrypt(request.getAlipayAccount().trim()));
        withdrawal.setStatus("PENDING");
        withdrawalMapper.insert(withdrawal);

        BigDecimal availableAfter = account.getAvailableBalance().subtract(amount);
        BigDecimal frozenAfter = account.getFrozenBalance().add(amount);
        account.setAvailableBalance(availableAfter);
        account.setFrozenBalance(frozenAfter);
        account.setVersion(account.getVersion() + 1);
        accountMapper.updateById(account);
        insertLedger(userId, "WITHDRAW_FREEZE", withdrawal.getWithdrawalNo(), amount.negate(), amount,
                availableAfter, frozenAfter, "提现申请冻结");
        eventPublisher.publishEvent(new UserNotificationEvent(userId, "PAYMENT", "提现申请已提交",
                "提现金额：" + amount.toPlainString() + "元，当前状态：审核中",
                "WALLET_WITHDRAWAL", withdrawal.getWithdrawalNo(), "WITHDRAW_NOTICE:PENDING:" + withdrawal.getWithdrawalNo()));
        return withdrawalVO(withdrawal);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WithdrawalVO> userWithdrawals(Long userId, PageQuery query) {
        return withdrawalPage(query, new LambdaQueryWrapper<Withdrawal>().eq(Withdrawal::getUserId, userId)
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Withdrawal::getStatus, query.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WithdrawalVO> adminWithdrawals(PageQuery query) {
        return withdrawalPage(query, new LambdaQueryWrapper<Withdrawal>()
                .eq(query.getUserId() != null, Withdrawal::getUserId, query.getUserId())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Withdrawal::getStatus, query.getStatus())
                .eq(query.getWithdrawalNo() != null && !query.getWithdrawalNo().isBlank(), Withdrawal::getWithdrawalNo, query.getWithdrawalNo()));
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawalVO adminWithdrawal(Long id) {
        Withdrawal withdrawal = withdrawalMapper.selectById(id);
        if (withdrawal == null) throw new ServiceException(404, "提现单不存在");
        return withdrawalVO(withdrawal, false);
    }

    @Override
    @Transactional
    public WithdrawalVO handleWithdrawal(Long id, HandleRequest request, Long adminId) {
        Withdrawal withdrawal = withdrawalMapper.selectForUpdate(id);
        if (withdrawal == null) throw new ServiceException(404, "提现单不存在");
        if (!"PENDING".equals(withdrawal.getStatus())) throw new ServiceException(409, "提现单已处理，不能重复操作");
        Account account = lockAccount(withdrawal.getUserId());
        if (account.getFrozenBalance().compareTo(withdrawal.getAmount()) < 0) {
            throw new ServiceException(409, "钱包冻结余额异常，请人工核查");
        }
        boolean success = "SUCCESS".equals(request.getAction());
        if (success && (request.getExternalTradeNo() == null || request.getExternalTradeNo().isBlank())) {
            throw new ServiceException(400, "确认提现成功前必须填写线下真实支付宝转账流水号");
        }
        BigDecimal frozenAfter = account.getFrozenBalance().subtract(withdrawal.getAmount());
        BigDecimal availableAfter = success ? account.getAvailableBalance() : account.getAvailableBalance().add(withdrawal.getAmount());
        account.setFrozenBalance(frozenAfter);
        account.setAvailableBalance(availableAfter);
        account.setVersion(account.getVersion() + 1);
        accountMapper.updateById(account);

        withdrawal.setStatus(success ? "SUCCEEDED" : "REJECTED");
        withdrawal.setExternalTradeNo(success ? request.getExternalTradeNo().trim() : null);
        withdrawal.setHandleNote(request.getNote());
        withdrawal.setHandledBy(adminId);
        withdrawal.setHandledAt(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);
        insertLedger(withdrawal.getUserId(), success ? "WITHDRAW_SUCCESS" : "WITHDRAW_REJECT",
                withdrawal.getWithdrawalNo(), success ? BigDecimal.ZERO : withdrawal.getAmount(),
                withdrawal.getAmount().negate(), availableAfter, frozenAfter,
                success ? "管理员确认线下支付宝转账成功" : "提现拒绝并解冻");
        eventPublisher.publishEvent(new UserNotificationEvent(withdrawal.getUserId(), "PAYMENT",
                success ? "提现已成功" : "提现申请已拒绝",
                success ? "运营人员已确认线下支付宝转账，金额：" + withdrawal.getAmount().toPlainString() + "元"
                        : "提现已拒绝并解冻，金额：" + withdrawal.getAmount().toPlainString() + "元"
                                + (request.getNote() == null || request.getNote().isBlank() ? "" : "，原因：" + request.getNote()),
                "WALLET_WITHDRAWAL", withdrawal.getWithdrawalNo(),
                "WITHDRAW_NOTICE:" + withdrawal.getStatus() + ":" + withdrawal.getWithdrawalNo()));
        return withdrawalVO(withdrawal);
    }

    private Account lockAccount(Long userId) {
        accountMapper.createIgnore(userId);
        Account account = accountMapper.selectForUpdate(userId);
        if (account == null) throw new ServiceException(500, "钱包账户创建失败");
        return account;
    }

    private void insertLedger(Long userId, String bizType, String bizId, BigDecimal availableChange,
                              BigDecimal frozenChange, BigDecimal availableAfter, BigDecimal frozenAfter, String remark) {
        Ledger ledger = new Ledger();
        ledger.setUserId(userId);
        ledger.setBizType(bizType);
        ledger.setBizId(bizId);
        ledger.setAvailableChange(availableChange);
        ledger.setFrozenChange(frozenChange);
        ledger.setAvailableAfter(availableAfter);
        ledger.setFrozenAfter(frozenAfter);
        ledger.setRemark(remark == null ? "" : remark);
        ledgerMapper.insert(ledger);
    }

    private PageResult<WithdrawalVO> withdrawalPage(PageQuery query, LambdaQueryWrapper<Withdrawal> wrapper) {
        Page<Withdrawal> result = withdrawalMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper.orderByDesc(Withdrawal::getId));
        return PageResult.of(result.getRecords().stream().map(this::withdrawalVO).toList(), result.getTotal(), query.getPage(), query.getSize());
    }

    private WithdrawalVO withdrawalVO(Withdrawal withdrawal) {
        return withdrawalVO(withdrawal, true);
    }

    private WithdrawalVO withdrawalVO(Withdrawal withdrawal, boolean mask) {
        WithdrawalVO vo = new WithdrawalVO();
        BeanUtils.copyProperties(withdrawal, vo, "alipayAccount");
        String alipayAccount = cryptoService.decrypt(withdrawal.getAlipayAccount());
        vo.setAlipayAccount(mask ? maskAlipayAccount(alipayAccount) : alipayAccount);
        return vo;
    }

    static String maskAlipayAccount(String account) {
        if (account == null || account.isBlank()) return account;
        int at = account.indexOf('@');
        if (at > 0) {
            String name = account.substring(0, at);
            int visible = Math.min(2, name.length());
            return name.substring(0, visible) + "***" + account.substring(at);
        }
        if (account.length() <= 5) return account.charAt(0) + "***" + account.substring(account.length() - 1);
        return account.substring(0, 3) + "****" + account.substring(account.length() - 2);
    }
}
