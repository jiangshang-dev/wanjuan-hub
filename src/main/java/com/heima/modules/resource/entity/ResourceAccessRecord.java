package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_access_record")
@EqualsAndHashCode(callSuper = true)
public class ResourceAccessRecord extends BaseEntity {
    private Long userId;
    private Long resourceId;
    private String targetType;
    private Long targetId;
}
