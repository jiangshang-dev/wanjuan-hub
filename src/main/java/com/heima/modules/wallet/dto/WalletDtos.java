package com.heima.modules.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

public final class WalletDtos {
    private WalletDtos() {
    }

    @Data
    @Schema(description = "钱包流水和提现分页查询")
    public static class PageQuery {
        @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
        @Min(1) @Max(100) @Schema(description = "每页数量", defaultValue = "20") private long size = 20;
        @Pattern(regexp = "PENDING|SUCCEEDED|REJECTED") @Schema(description = "提现状态，仅提现列表有效") private String status;
        @Size(max = 64) @Schema(description = "提现单号，仅后台可用") private String withdrawalNo;
        @Schema(description = "用户ID，仅后台可用") private Long userId;
    }

    @Data
    @Schema(description = "支付宝提现申请")
    public static class WithdrawRequest {
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) @Schema(description = "提现金额，单位元") private BigDecimal amount;
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._@-]{5,128}$") @Schema(description = "支付宝手机号或邮箱账号，响应中会脱敏") private String alipayAccount;
    }

    @Data
    @Schema(description = "管理员人工确认提现结果")
    public static class HandleRequest {
        @NotBlank @Pattern(regexp = "SUCCESS|REJECT") @Schema(description = "人工确认动作", allowableValues = {"SUCCESS", "REJECT"}) private String action;
        @Size(max = 128) @Schema(description = "线下真实支付宝转账流水号；确认成功时必填，系统不会发起第三方打款") private String externalTradeNo;
        @Size(max = 1000) @Schema(description = "人工处理备注或拒绝原因") private String note;
    }
}
