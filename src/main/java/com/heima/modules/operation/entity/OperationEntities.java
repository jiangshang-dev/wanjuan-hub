package com.heima.modules.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

public final class OperationEntities {
    private OperationEntities() {}

    @Data @TableName("operation_notice") @EqualsAndHashCode(callSuper = true)
    public static class Announcement extends BaseEntity {
        private String title;
        private String content;
        private Integer status;
        private LocalDateTime publishStart;
        private LocalDateTime publishEnd;
        private Integer sortOrder;
    }

    @Data @TableName("operation_recommend") @EqualsAndHashCode(callSuper = true)
    public static class Recommendation extends BaseEntity {
        private String contentType;
        private Long contentId;
        private String title;
        private String reason;
        private Integer status;
        private Integer sortOrder;
    }

    @Data @TableName("operation_advertisement") @EqualsAndHashCode(callSuper = true)
    public static class Advertisement extends BaseEntity {
        private String title;
        private String imageUrl;
        private String targetUrl;
        private String position;
        private Integer status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer sortOrder;
    }

    @Data @TableName("operation_friend_link") @EqualsAndHashCode(callSuper = true)
    public static class FriendLink extends BaseEntity {
        private String name;
        private String url;
        private String logoUrl;
        private Integer status;
        private Integer sortOrder;
    }

    @Data @TableName("search_hot_keyword") @EqualsAndHashCode(callSuper = true)
    public static class HotKeyword extends BaseEntity {
        private String keyword;
        private Long heat;
        private Integer status;
        private Integer sortOrder;
    }

    @Data @TableName("sys_config") @EqualsAndHashCode(callSuper = true)
    public static class SiteConfig extends BaseEntity {
        private String configKey;
        private String configValue;
        private String description;
        private Integer publicFlag;
        private Integer status;
    }
}
