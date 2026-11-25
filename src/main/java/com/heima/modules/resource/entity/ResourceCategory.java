package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_category")
@EqualsAndHashCode(callSuper = true)
public class ResourceCategory extends BaseEntity {
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
