package com.heima.modules.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class WalletVOs {
    private WalletVOs() {
    }

    @Data
    @Schema(description = "独立返佣钱包账户")
    public static class AccountVO {
        @Schema(description = "用户ID") private Long userId;
        @Schema(description = "可提现余额") private BigDecimal availableBalance;
        @Schema(description = "提现审核冻结金额") private BigDecimal frozenBalance;
        @Schema(description = "累计返佣收入") private BigDecimal totalIncome;
    }

    @Data
    @Schema(description = "不可变钱包流水")
    public static class LedgerVO {
        @Schema(description = "流水ID") private Long id;
        @Schema(description = "用户ID") private Long userId;
        @Schema(description = "业务类型") private String bizType;
        @Schema(description = "业务幂等标识") private String bizId;
        @Schema(description = "可用余额变动") private BigDecimal availableChange;
        @Schema(description = "冻结余额变动") private BigDecimal frozenChange;
        @Schema(description = "变动后可用余额") private BigDecimal availableAfter;
        @Schema(description = "变动后冻结余额") private BigDecimal frozenAfter;
        @Schema(description = "摘要") private String remark;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data
    @Schema(description = "人工提现单")
    public static class WithdrawalVO {
        @Schema(description = "提现单ID") private Long id;
        @Schema(description = "提现单号") private String withdrawalNo;
        @Schema(description = "用户ID") private Long userId;
        @Schema(description = "提现金额") private BigDecimal amount;
        @Schema(description = "脱敏后的支付宝账号") private String alipayAccount;
        @Schema(description = "状态：PENDING审核中，SUCCEEDED人工确认成功，REJECTED已拒绝并解冻") private String status;
        @Schema(description = "管理员填写的线下真实支付宝转账流水号") private String externalTradeNo;
        @Schema(description = "处理备注或拒绝原因") private String handleNote;
        @Schema(description = "处理管理员ID") private Long handledBy;
        @Schema(description = "人工处理时间") private LocalDateTime handledAt;
        @Schema(description = "申请时间") private LocalDateTime createTime;
    }
}
