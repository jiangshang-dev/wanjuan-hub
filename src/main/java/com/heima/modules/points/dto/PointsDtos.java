package com.heima.modules.points.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class PointsDtos {
    private PointsDtos() {}

    @Data
    public static class RuleRequest {
        @NotBlank @Size(max = 40) @Schema(description = "规则编码/审核业务类型") private String code;
        @NotBlank @Size(max = 80) @Schema(description = "规则名称") private String name;
        @NotNull @Min(0) @Schema(description = "奖励积分") private Integer points;
        @NotNull @Min(0) @Schema(description = "奖励经验") private Integer experience;
        @NotNull @Min(0) @Schema(description = "每日获取上限，0为不限") private Integer dailyLimit;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @Size(max = 500) @Schema(description = "说明") private String remark = "";
    }

    @Data
    public static class LevelRequest {
        @NotNull @Min(1) @Schema(description = "等级") private Integer level;
        @NotBlank @Size(max = 50) @Schema(description = "等级名称") private String name;
        @NotNull @Min(0) @Schema(description = "最低经验") private Long minExperience;
        @Size(max = 500) @Schema(description = "图标地址") private String icon;
        @Size(max = 1000) @Schema(description = "权益说明") private String benefits = "";
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @NotNull @Schema(description = "排序") private Integer sortOrder = 0;
    }

    @Data @Schema(description = "连续签到奖励档位")
    public static class StreakRuleRequest {
        @NotNull @Min(1) @Max(3650) @Schema(description = "连续签到第N天") private Integer streakDay;
        @NotNull @Min(0) @Schema(description = "该档总奖励积分") private Integer points;
        @NotNull @Min(0) @Schema(description = "该档总奖励经验") private Integer experience;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @Size(max = 500) @Schema(description = "说明") private String remark = "";
    }

    @Data
    public static class AdjustmentRequest {
        @NotNull @Schema(description = "用户ID") private Long userId;
        @NotNull @Schema(description = "积分调整值，可为负数") private Integer points;
        @NotNull @Schema(description = "经验调整值，可为负数") private Integer experience = 0;
        @NotBlank @Size(max = 500) @Schema(description = "调整原因") private String reason;
        @NotBlank @Size(max = 120) @Schema(description = "管理端幂等键") private String idempotencyKey;
    }
}
