package com.heima.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@TableName("audit_task")
@EqualsAndHashCode(callSuper = true)
public class AuditTask extends BaseEntity {
    private String bizType;
    private Long bizId;
    private Long authorId;
    private Integer versionNo;
    private String status;
    private String reason;
    private Integer retryCount;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private LocalDateTime finishedAt;
}
