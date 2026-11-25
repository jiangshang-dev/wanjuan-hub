package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_tag_relation")
@EqualsAndHashCode(callSuper = true)
public class ResourceTagRelation extends BaseEntity {
    private Long resourceId;
    private Long tagId;
}
