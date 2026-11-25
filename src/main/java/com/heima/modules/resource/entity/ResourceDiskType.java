package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_disk_type")
@EqualsAndHashCode(callSuper = true)
public class ResourceDiskType extends BaseEntity {
    private String code;
    private String name;
    private String allowedHosts;
    private Integer sortOrder;
    private Integer status;
}
