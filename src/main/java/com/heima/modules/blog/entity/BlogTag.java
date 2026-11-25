package com.heima.modules.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("blog_tag")
@EqualsAndHashCode(callSuper = true)
public class BlogTag extends BaseEntity {
    private String name;
}
