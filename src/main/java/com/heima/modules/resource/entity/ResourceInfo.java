package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resource_info")
@EqualsAndHashCode(callSuper = true)
public class ResourceInfo extends BaseEntity {
    private Long authorId;
    private Long categoryId;
    private String status;
    private Integer latestVersionNo;
    private Integer publishedVersionNo;
    private String auditReason;
    private Integer online;
    private Integer invalid;
    private Long viewCount;
    private Long commentCount;
    private Integer deleted;
}
