package com.heima.modules.wallet;

import com.heima.exception.ServiceException;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.resource.service.ResourceCryptoService;
import com.heima.modules.wallet.dto.WalletDtos.HandleRequest;
import com.heima.modules.wallet.dto.WalletDtos.WithdrawRequest;
import com.heima.modules.wallet.entity.WalletEntities.Account;
import com.heima.modules.wallet.entity.WalletEntities.Ledger;
import com.heima.modules.wallet.entity.WalletEntities.Withdrawal;
import com.heima.modules.wallet.mapper.WalletMappers.AccountMapper;
import com.heima.modules.wallet.mapper.WalletMappers.LedgerMapper;
import com.heima.modules.wallet.mapper.WalletMappers.WithdrawalMapper;
import com.heima.modules.wallet.service.impl.WalletServiceImpl;
import com.heima.modules.wallet.vo.WalletVOs.WithdrawalVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {
    @Mock private AccountMapper accountMapper;
    @Mock private LedgerMapper ledgerMapper;
    @Mock private WithdrawalMapper withdrawalMapper;
    @Mock private ResourceCryptoService cryptoService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private WalletServiceImpl walletService;

    @Test
    void commissionCreditIsIdempotentAndWritesImmutableLedgerOnce() {
        Account account = account("100.0000", "0.0000");
        when(accountMapper.selectForUpdate(8L)).thenReturn(account);
        when(ledgerMapper.selectByBiz("POINTS_RESOURCE_COMMISSION", "8:1"))
                .thenReturn(null, null, new Ledger());

        walletService.credit(8L, "POINTS_RESOURCE_COMMISSION", "8:1", new BigDecimal("3.00"), "积分返佣");
        walletService.credit(8L, "POINTS_RESOURCE_COMMISSION", "8:1", new BigDecimal("3.00"), "积分返佣");

        assertEquals(new BigDecimal("103.0000"), account.getAvailableBalance());
        assertEquals(new BigDecimal("103.0000"), account.getTotalIncome());
        verify(accountMapper, times(1)).updateById(account);
        verify(ledgerMapper, times(1)).insert(any(Ledger.class));
    }

    @Test
    void withdrawalApplicationFreezesBalanceAndMasksAlipayAccount() {
        Account account = account("100.00", "0.00");
        when(accountMapper.selectForUpdate(8L)).thenReturn(account);
        when(cryptoService.encrypt("author@example.com")).thenReturn("encrypted-account");
        when(cryptoService.decrypt("encrypted-account")).thenReturn("author@example.com");
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(new BigDecimal("50.50"));
        request.setAlipayAccount("author@example.com");

        WithdrawalVO result = walletService.applyWithdrawal(8L, request);

        assertEquals(new BigDecimal("49.50"), account.getAvailableBalance());
        assertEquals(new BigDecimal("50.50"), account.getFrozenBalance());
        assertEquals("au***@example.com", result.getAlipayAccount());
        assertEquals("PENDING", result.getStatus());
        ArgumentCaptor<Withdrawal> withdrawal = ArgumentCaptor.forClass(Withdrawal.class);
        verify(withdrawalMapper).insert(withdrawal.capture());
        assertEquals("encrypted-account", withdrawal.getValue().getAlipayAccount());
        ArgumentCaptor<Ledger> ledger = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerMapper).insert(ledger.capture());
        assertEquals("WITHDRAW_FREEZE", ledger.getValue().getBizType());
        assertEquals(new BigDecimal("-50.50"), ledger.getValue().getAvailableChange());
    }

    @Test
    void rejectedWithdrawalUnfreezesFullAmount() {
        Account account = account("60.00", "40.00");
        Withdrawal withdrawal = withdrawal("40.00");
        when(withdrawalMapper.selectForUpdate(55L)).thenReturn(withdrawal);
        when(accountMapper.selectForUpdate(8L)).thenReturn(account);
        HandleRequest request = new HandleRequest();
        request.setAction("REJECT");
        request.setNote("账号信息有误");

        WithdrawalVO result = walletService.handleWithdrawal(55L, request, 1L);

        assertEquals("REJECTED", result.getStatus());
        assertEquals(new BigDecimal("100.00"), account.getAvailableBalance());
        assertEquals(new BigDecimal("0.00"), account.getFrozenBalance());
    }

    @Test
    void successRequiresRealOfflineTradeNumberAndNeverSimulatesPayment() {
        Withdrawal withdrawal = withdrawal("40.00");
        when(withdrawalMapper.selectForUpdate(55L)).thenReturn(withdrawal);
        when(accountMapper.selectForUpdate(8L)).thenReturn(account("60.00", "40.00"));
        HandleRequest request = new HandleRequest();
        request.setAction("SUCCESS");

        ServiceException error = assertThrows(ServiceException.class, () -> walletService.handleWithdrawal(55L, request, 1L));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("真实支付宝转账流水号"));
        verify(withdrawalMapper, never()).updateById(any(Withdrawal.class));
        verify(ledgerMapper, never()).insert(any(Ledger.class));
    }

    @Test
    void adminDetailDecryptsFullAlipayAccountForOfflineTransfer() {
        Withdrawal withdrawal = withdrawal("40.00");
        withdrawal.setAlipayAccount("encrypted-account");
        when(withdrawalMapper.selectById(55L)).thenReturn(withdrawal);
        when(cryptoService.decrypt("encrypted-account")).thenReturn("13800138000");

        WithdrawalVO result = walletService.adminWithdrawal(55L);

        assertEquals("13800138000", result.getAlipayAccount());
    }

    @Test
    void successfulManualConfirmationReleasesFrozenBalanceAndPublishesNotification() {
        Account account = account("60.00", "40.00");
        Withdrawal withdrawal = withdrawal("40.00");
        withdrawal.setAlipayAccount("encrypted-account");
        when(withdrawalMapper.selectForUpdate(55L)).thenReturn(withdrawal);
        when(accountMapper.selectForUpdate(8L)).thenReturn(account);
        when(cryptoService.decrypt("encrypted-account")).thenReturn("13800138000");
        HandleRequest request = new HandleRequest();
        request.setAction("SUCCESS");
        request.setExternalTradeNo("ALIPAY-REAL-TRADE-1");

        WithdrawalVO result = walletService.handleWithdrawal(55L, request, 1L);

        assertEquals("SUCCEEDED", result.getStatus());
        assertEquals(new BigDecimal("60.00"), account.getAvailableBalance());
        assertEquals(new BigDecimal("0.00"), account.getFrozenBalance());
        assertEquals("ALIPAY-REAL-TRADE-1", result.getExternalTradeNo());
        ArgumentCaptor<UserNotificationEvent> event = ArgumentCaptor.forClass(UserNotificationEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("WITHDRAW_NOTICE:SUCCEEDED:W1", event.getValue().dedupKey());
    }

    private Account account(String available, String frozen) {
        Account account = new Account();
        account.setUserId(8L);
        account.setAvailableBalance(new BigDecimal(available));
        account.setFrozenBalance(new BigDecimal(frozen));
        account.setTotalIncome(new BigDecimal("100.0000"));
        account.setVersion(0);
        return account;
    }

    private Withdrawal withdrawal(String amount) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(55L);
        withdrawal.setWithdrawalNo("W1");
        withdrawal.setUserId(8L);
        withdrawal.setAmount(new BigDecimal(amount));
        withdrawal.setAlipayAccount("13800138000");
        withdrawal.setStatus("PENDING");
        return withdrawal;
    }
}
