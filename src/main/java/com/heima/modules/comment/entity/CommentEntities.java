package com.heima.modules.comment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class CommentEntities {
    private CommentEntities() {}

    @Data @TableName("content_comment") @EqualsAndHashCode(callSuper = true)
    public static class Comment extends BaseEntity {
        private String targetType;
        private Long targetId;
        private Long authorId;
        private Long rootId;
        private Long parentId;
        private Long replyToId;
        private String status;
        private Integer latestVersionNo;
        private Integer publishedVersionNo;
        private String auditReason;
        private Long replyCount;
        private Long likeCount;
        private Integer deleted;
    }

    @Data @TableName("content_comment_version") @EqualsAndHashCode(callSuper = true)
    public static class Version extends BaseEntity {
        private Long commentId;
        private Integer versionNo;
        private String content;
        private String contentFileIds;
        private String status;
        private String auditReason;
    }
}
