package com.heima.modules.points.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public final class PointsVOs {
    private PointsVOs() {}

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AccountVO {
        @Schema(description = "用户ID") private Long userId;
        @Schema(description = "可用积分") private Integer balance;
        @Schema(description = "累计积分") private Long totalPoints;
        @Schema(description = "当前经验") private Long experience;
        @Schema(description = "今日是否已签到") private Boolean signedToday;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RecordVO {
        @Schema(description = "流水ID") private Long id;
        @Schema(description = "积分变动") private Integer changeAmount;
        @Schema(description = "变动后余额") private Integer balanceAfter;
        @Schema(description = "经验变动") private Integer experienceChange;
        @Schema(description = "变动后经验") private Long experienceAfter;
        @Schema(description = "业务类型") private String bizType;
        @Schema(description = "业务ID") private Long bizId;
        @Schema(description = "说明") private String remark;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LevelVO {
        @Schema(description = "等级") private Integer level;
        @Schema(description = "名称") private String name;
        @Schema(description = "最低经验") private Long minExperience;
        @Schema(description = "图标") private String icon;
        @Schema(description = "权益") private String benefits;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MembershipVO {
        @Schema(description = "当前经验") private Long experience;
        @Schema(description = "当前等级") private LevelVO currentLevel;
        @Schema(description = "下一等级") private LevelVO nextLevel;
        @Schema(description = "距下一等级经验") private Long experienceToNext;
    }
}
