package com.heima.modules.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("blog_category")
@EqualsAndHashCode(callSuper = true)
public class BlogCategory extends BaseEntity {
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
