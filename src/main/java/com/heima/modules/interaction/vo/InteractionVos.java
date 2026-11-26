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
        @Schema(description = "反应类型：LIKE/DISLIKE") private String reactionType;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }

    @Data @Schema(description = "目标赞踩状态")
    public static class ReactionVO {
        @Schema(description = "目标类型") private String targetType;
        @Schema(description = "目标ID") private Long targetId;
        @Schema(description = "点赞数") private Long likeCount;
        @Schema(description = "点踩数") private Long dislikeCount;
        @Schema(description = "当前用户反应：LIKE/DISLIKE/NONE") private String userReaction;
        @Schema(description = "作者是否允许点赞") private Boolean likeAllowed;
        @Schema(description = "作者是否允许点踩") private Boolean dislikeAllowed;
        @Schema(description = "当前是否灰色日") private Boolean grayDay;
        @Schema(description = "灰色日原因") private String grayReason;
    }

    @Data @Schema(description = "关注关系")
    public static class FollowVO {
        @Schema(description = "记录ID") private Long id;
        @Schema(description = "关注人ID") private Long followerId;
        @Schema(description = "被关注人ID") private Long followeeId;
        @Schema(description = "创建时间") private LocalDateTime createTime;
    }
}
