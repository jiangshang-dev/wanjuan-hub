package com.heima.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    private Long parentId;
    private String name;
    private String code;
    private String type;
    private String path;
    private String icon;
    private Integer sortOrder;
    private Integer status;
}
