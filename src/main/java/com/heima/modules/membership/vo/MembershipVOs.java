package com.heima.modules.membership.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MembershipVOs {
    private MembershipVOs() {
    }

    @Data
    @Schema(description = "付费会员套餐")
    public static class PlanVO {
        @Schema(description = "套餐ID") private Long id;
        @Schema(description = "套餐名称") private String name;
        @Schema(description = "套餐说明") private String description;
        @Schema(description = "有效天数") private Integer durationDays;
        @Schema(description = "套餐价格，单位元") private BigDecimal price;
        @Schema(description = "每日可领取会员资源数") private Integer dailyResourceLimit;
        @Schema(description = "状态：0停用，1启用") private Integer status;
        @Schema(description = "排序值") private Integer sortOrder;
        @Schema(description = "创建时间") private LocalDateTime createTime;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }

    @Data
    @Schema(description = "当前用户会员状态")
    public static class StatusVO {
        @Schema(description = "用户ID") private Long userId;
        @Schema(description = "当前是否有效") private Boolean active;
        @Schema(description = "当前套餐ID") private Long planId;
        @Schema(description = "当前套餐名称") private String planName;
        @Schema(description = "会员到期时间") private LocalDateTime expireAt;
        @Schema(description = "每日可领取会员资源数") private Integer dailyResourceLimit;
        @Schema(description = "业务日期，固定Asia/Shanghai自然日") private LocalDate claimDate;
        @Schema(description = "今日已领取数量") private Integer usedToday;
        @Schema(description = "今日剩余可领取数量") private Integer remainingToday;
    }

    @Data
    @Schema(description = "资源返佣配置")
    public static class CommissionConfigVO {
        @Schema(description = "每扣除1积分返给作者的金额，单位元") private BigDecimal pointsUnitPrice;
        @Schema(description = "会员资源首次有效领取固定返佣，单位元") private BigDecimal memberFixedAmount;
        @Schema(description = "更新时间") private LocalDateTime updateTime;
    }
}
