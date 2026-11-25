package com.heima.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("mq_consume_log")
@EqualsAndHashCode(callSuper = true)
public class MqConsumeLog extends BaseEntity {
    private String messageId;
    private String consumerName;
    private String status;
    private String errorMessage;
    private LocalDateTime processedAt;
}
