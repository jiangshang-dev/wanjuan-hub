package com.heima.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class InteractionEntities {
    private InteractionEntities() {}

    @Data @TableName("content_like") @EqualsAndHashCode(callSuper = true)
    public static class Like extends BaseEntity {
        private Long userId;
        private String targetType;
        private Long targetId;
    }

    @Data @TableName("content_favorite") @EqualsAndHashCode(callSuper = true)
    public static class Favorite extends BaseEntity {
        private Long userId;
        private String targetType;
        private Long targetId;
    }

    @Data @TableName("user_follow") @EqualsAndHashCode(callSuper = true)
    public static class Follow extends BaseEntity {
        private Long followerId;
        private Long followeeId;
    }
}
