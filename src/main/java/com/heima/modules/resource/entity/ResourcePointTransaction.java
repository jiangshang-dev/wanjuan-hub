package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_point_transaction")
@EqualsAndHashCode(callSuper = true)
public class ResourcePointTransaction extends BaseEntity {
    private Long userId;
    private Long resourceId;
    private Integer changeAmount;
    private Integer balanceAfter;
    private String bizType;
}
