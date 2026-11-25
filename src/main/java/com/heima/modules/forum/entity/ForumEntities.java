package com.heima.modules.forum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class ForumEntities {
    private ForumEntities() {}

    @Data @TableName("forum_section") @EqualsAndHashCode(callSuper = true)
    public static class Section extends BaseEntity {
        private String name;
        private Integer sortOrder;
        private Integer status;
    }

    @Data @TableName("forum_board") @EqualsAndHashCode(callSuper = true)
    public static class Board extends BaseEntity {
        private Long sectionId;
        private Long parentId;
        private String name;
        private String description;
        private Integer sortOrder;
        private Integer status;
        private String postPermission;
        private String replyPermission;
        private Long topicCount;
    }

    @Data @TableName("forum_moderator") @EqualsAndHashCode(callSuper = true)
    public static class Moderator extends BaseEntity {
        private Long boardId;
        private Long adminId;
        private Integer status;
    }

    @Data @TableName("forum_topic") @EqualsAndHashCode(callSuper = true)
    public static class Topic extends BaseEntity {
        private Long boardId;
        private Long authorId;
        private String status;
        private Integer latestVersionNo;
        private Integer publishedVersionNo;
        private String auditReason;
        private Integer locked;
        private Integer topped;
        private Integer essence;
        private Long postCount;
        private Long commentCount;
        private Long likeCount;
        private Long dislikeCount;
        private Long favoriteCount;
        private Long viewCount;
        private Integer deleted;
    }

    @Data @TableName("forum_topic_version") @EqualsAndHashCode(callSuper = true)
    public static class TopicVersion extends BaseEntity {
        private Long topicId;
        private Integer versionNo;
        private String title;
        private String content;
        private String contentFileIds;
        private String status;
        private String auditReason;
    }

    @Data @TableName("forum_post") @EqualsAndHashCode(callSuper = true)
    public static class Post extends BaseEntity {
        private Long topicId;
        private Long boardId;
        private Long authorId;
        private Long rootId;
        private Long parentId;
        private Long replyToId;
        private Integer floorNo;
        private String status;
        private Integer latestVersionNo;
        private Integer publishedVersionNo;
        private String auditReason;
        private Long likeCount;
        private Long dislikeCount;
        private Integer deleted;
    }

    @Data @TableName("forum_post_version") @EqualsAndHashCode(callSuper = true)
    public static class PostVersion extends BaseEntity {
        private Long postId;
        private Integer versionNo;
        private String content;
        private String contentFileIds;
        private String status;
        private String auditReason;
    }
}
