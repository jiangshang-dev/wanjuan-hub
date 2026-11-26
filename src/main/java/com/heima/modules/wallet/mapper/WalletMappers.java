package com.heima.modules.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.wallet.entity.WalletEntities.Account;
import com.heima.modules.wallet.entity.WalletEntities.Ledger;
import com.heima.modules.wallet.entity.WalletEntities.Withdrawal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public final class WalletMappers {
    private WalletMappers() {
    }

    public interface AccountMapper extends BaseMapper<Account> {
        @Insert("INSERT IGNORE INTO wallet_account(user_id, available_balance, frozen_balance, total_income, version) VALUES(#{userId}, 0, 0, 0, 0)")
        int createIgnore(@Param("userId") Long userId);

        @Select("SELECT * FROM wallet_account WHERE user_id=#{userId} FOR UPDATE")
        Account selectForUpdate(@Param("userId") Long userId);
    }

    public interface LedgerMapper extends BaseMapper<Ledger> {
        @Select("SELECT * FROM wallet_ledger WHERE biz_type=#{bizType} AND biz_id=#{bizId}")
        Ledger selectByBiz(@Param("bizType") String bizType, @Param("bizId") String bizId);
    }

    public interface WithdrawalMapper extends BaseMapper<Withdrawal> {
        @Select("SELECT * FROM wallet_withdrawal WHERE id=#{id} FOR UPDATE")
        Withdrawal selectForUpdate(@Param("id") Long id);
    }
}
