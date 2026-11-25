package com.heima.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("audit_item")
@EqualsAndHashCode(callSuper = true)
public class AuditItem extends BaseEntity {
    private Long taskId;
    private String itemType;
    private String content;
    private String status;
    private String suggestion;
    private String reason;
    private String providerRequestId;
}
