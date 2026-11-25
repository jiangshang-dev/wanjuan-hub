package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("resource_entitlement")
@EqualsAndHashCode(callSuper = true)
public class ResourceEntitlement extends BaseEntity {
    private Long userId;
    private Long resourceId;
    private String sourceType;
    private String sourceOrderNo;
    private BigDecimal amount;
    private Integer pointsAmount;
    private String status;
    private String revokeReason;
    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;
}
