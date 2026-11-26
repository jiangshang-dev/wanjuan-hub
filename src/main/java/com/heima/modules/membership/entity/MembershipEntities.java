package com.heima.modules.membership.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MembershipEntities {
    private MembershipEntities() {
    }

    @Data
    @TableName("membership_plan")
    @EqualsAndHashCode(callSuper = true)
    public static class Plan extends BaseEntity {
        private String name;
        private String description;
        private Integer durationDays;
        private BigDecimal price;
        private Integer dailyResourceLimit;
        private Integer status;
        private Integer sortOrder;
    }

    @Data
    @TableName("user_membership")
    public static class UserMembership {
        @TableId(value = "user_id", type = IdType.INPUT)
        private Long userId;
        private Long planId;
        private Integer dailyResourceLimit;
        private LocalDateTime expireAt;
        private String lastOrderNo;
        private Integer version;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;
    }

    @Data
    @TableName("membership_daily_usage")
    public static class DailyUsage {
        private Long userId;
        private LocalDate claimDate;
        private Integer usedCount;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;
    }

    @Data
    @TableName("membership_resource_claim")
    @EqualsAndHashCode(callSuper = true)
    public static class ResourceClaim extends BaseEntity {
        private Long userId;
        private Long resourceId;
        private LocalDate claimDate;
    }

    @Data
    @TableName("membership_commission_config")
    @EqualsAndHashCode(callSuper = true)
    public static class CommissionConfig extends BaseEntity {
        private BigDecimal pointsUnitPrice;
        private BigDecimal memberFixedAmount;
    }
}
