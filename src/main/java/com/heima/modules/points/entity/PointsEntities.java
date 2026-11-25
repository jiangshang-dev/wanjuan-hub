package com.heima.modules.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

public final class PointsEntities {
    private PointsEntities() {}

    @Data
    @TableName("points_account")
    public static class Account {
        @TableId(value = "user_id", type = IdType.INPUT)
        private Long userId;
        private Integer balance;
        private Long totalPoints;
        private Long experience;
        private Integer version;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }

    @Data @TableName("points_record") @EqualsAndHashCode(callSuper = true)
    public static class Record extends BaseEntity {
        private Long userId;
        private Integer changeAmount;
        private Integer balanceAfter;
        private Integer experienceChange;
        private Long experienceAfter;
        private String bizType;
        private Long bizId;
        private String idempotencyKey;
        private String remark;
    }

    @Data @TableName("points_rule") @EqualsAndHashCode(callSuper = true)
    public static class Rule extends BaseEntity {
        private String code;
        private String name;
        private Integer points;
        private Integer experience;
        private Integer dailyLimit;
        private Integer status;
        private String remark;
    }

    @Data @TableName("user_sign_in") @EqualsAndHashCode(callSuper = true)
    public static class SignIn extends BaseEntity {
        private Long userId;
        private LocalDate signDate;
        private Integer streakDays;
        private Integer points;
        private String idempotencyKey;
    }

    @Data @TableName("sign_in_streak_rule") @EqualsAndHashCode(callSuper = true)
    public static class StreakRule extends BaseEntity {
        private Integer streakDay;
        private Integer points;
        private Integer experience;
        private Integer status;
        private String remark;
    }

    @Data @TableName("user_level") @EqualsAndHashCode(callSuper = true)
    public static class Level extends BaseEntity {
        private Integer level;
        private String name;
        private Long minExperience;
        private String icon;
        private String benefits;
        private Integer status;
        private Integer sortOrder;
    }
}
