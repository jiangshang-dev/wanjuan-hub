package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_tag")
@EqualsAndHashCode(callSuper = true)
public class ResourceTag extends BaseEntity {
    private String name;
}
