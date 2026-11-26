package com.heima.modules.membership.dto;

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

public final class MembershipDtos {
    private MembershipDtos() {
    }

    @Data
    @Schema(description = "会员套餐保存请求")
    public static class PlanRequest {
        @NotBlank @Size(max = 100) @Schema(description = "套餐名称") private String name;
        @Size(max = 500) @Schema(description = "套餐说明") private String description;
        @NotNull @Min(1) @Max(3650) @Schema(description = "有效天数") private Integer durationDays;
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) @Schema(description = "套餐价格，单位元") private BigDecimal price;
        @NotNull @Min(1) @Max(10000) @Schema(description = "每个自然日可领取的会员资源数") private Integer dailyResourceLimit;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @Min(0) @Schema(description = "排序值") private Integer sortOrder;
    }

    @Data
    @Schema(description = "返佣配置请求")
    public static class CommissionConfigRequest {
        @NotNull @DecimalMin("0.00") @Digits(integer = 14, fraction = 4) @Schema(description = "每扣除1积分返给作者的金额，单位元") private BigDecimal pointsUnitPrice;
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) @Schema(description = "会员资源首次有效领取的固定返佣，单位元") private BigDecimal memberFixedAmount;
    }

    @Data
    @Schema(description = "会员套餐后台分页查询")
    public static class PlanQuery {
        @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
        @Min(1) @Max(100) @Schema(description = "每页数量", defaultValue = "20") private long size = 20;
        @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
    }

    @Data
    @Schema(description = "创建会员购买订单请求")
    public static class CreateOrderRequest {
        @NotNull @Schema(description = "会员套餐ID") private Long planId;
        @NotBlank @Pattern(regexp = "PC|H5") @Schema(description = "客户端类型", allowableValues = {"PC", "H5"}) private String clientType;
        @NotBlank @Size(min = 8, max = 48) @Schema(description = "客户端生成的幂等键") private String idempotencyKey;
    }
}
