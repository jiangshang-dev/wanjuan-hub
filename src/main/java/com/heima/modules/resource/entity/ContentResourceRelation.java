package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("content_resource_relation")
@EqualsAndHashCode(callSuper = true)
public class ContentResourceRelation extends BaseEntity {
    private String contentType;
    private Long contentId;
    private Long resourceId;
}
