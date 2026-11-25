package com.heima.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("audit_outbox")
@EqualsAndHashCode(callSuper = true)
public class AuditOutbox extends BaseEntity {
    private String messageId;
    private Long taskId;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private LocalDateTime sentAt;
    private String lastError;
}
