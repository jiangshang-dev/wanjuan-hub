package com.heima.modules.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data @TableName("operation_feedback") @EqualsAndHashCode(callSuper = true)
public class FeedbackEntity extends BaseEntity {
    private Long userId;
    private String content;
    private String status;
    private String adminReply;
    private Long replyAdminId;
    private LocalDateTime replyTime;
}
