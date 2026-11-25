package com.heima.modules.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

public final class MessageEntities {
    private MessageEntities() {}

    @Data @TableName("message_notification") @EqualsAndHashCode(callSuper = true)
    public static class Notification extends BaseEntity {
        private Long userId;
        private String type;
        private String title;
        private String content;
        private String bizType;
        private String bizId;
        private String dedupKey;
        private Integer readFlag;
        private LocalDateTime readTime;
    }

    @Data @TableName("message_preference")
    public static class Preference {
        @TableId(value = "user_id", type = IdType.INPUT)
        private Long userId;
        private Integer auditEnabled;
        private Integer paymentEnabled;
        private Integer systemEnabled;
        private Integer interactionEnabled;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
