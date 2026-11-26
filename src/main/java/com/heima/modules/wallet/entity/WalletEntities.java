package com.heima.modules.wallet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class WalletEntities {
    private WalletEntities() {
    }

    @Data
    @TableName("wallet_account")
    public static class Account {
        @TableId(value = "user_id", type = IdType.INPUT)
        private Long userId;
        private BigDecimal availableBalance;
        private BigDecimal frozenBalance;
        private BigDecimal totalIncome;
        private Integer version;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;
    }

    @Data
    @TableName("wallet_ledger")
    public static class Ledger {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        private String bizType;
        private String bizId;
        private BigDecimal availableChange;
        private BigDecimal frozenChange;
        private BigDecimal availableAfter;
        private BigDecimal frozenAfter;
        private String remark;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;
    }

    @Data
    @TableName("wallet_withdrawal")
    @EqualsAndHashCode(callSuper = true)
    public static class Withdrawal extends BaseEntity {
        private String withdrawalNo;
        private Long userId;
        private BigDecimal amount;
        private String alipayAccount;
        private String status;
        private String externalTradeNo;
        private String handleNote;
        private Long handledBy;
        private LocalDateTime handledAt;
    }
}
