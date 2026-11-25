package com.heima.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("audit_manual_record")
@EqualsAndHashCode(callSuper = true)
public class AuditManualRecord extends BaseEntity {
    private Long taskId;
    private Long reviewerId;
    private String decision;
    private String reason;
}
