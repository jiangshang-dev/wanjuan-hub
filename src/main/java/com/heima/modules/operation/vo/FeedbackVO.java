package com.heima.modules.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "意见反馈及管理员回复")
public record FeedbackVO(@Schema(description = "反馈ID，字符串") String id,
        @Schema(description = "反馈用户ID，字符串") String userId,
        @Schema(description = "反馈内容") String content,
        @Schema(description = "状态：PENDING待回复，REPLIED已回复，CLOSED已关闭") String status,
        @Schema(description = "管理员回复，未回复为null") String adminReply,
        @Schema(description = "回复时间") LocalDateTime replyTime,
        @Schema(description = "提交时间") LocalDateTime createTime,
        @Schema(description = "更新时间") LocalDateTime updateTime) {}
