package com.heima.modules.interaction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

public final class InteractionVos {
    private InteractionVos() {}

    @Data @Schema(description = "点赞或收藏记录")
    public static class TargetVO {
        @Schema(description = "记录ID") private Long id;
        @Schema(description = "用户ID") private Long userId;
        @Schema(description = "目标类型") private String targetType;
        @Schema(description = "目标ID") private Long targetId;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data @Schema(description = "关注关系")
    public static class FollowVO {
        @Schema(description = "记录ID") private Long id;
        @Schema(description = "关注人ID") private Long followerId;
        @Schema(description = "被关注人ID") private Long followeeId;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }
}
