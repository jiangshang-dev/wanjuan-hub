package com.heima.modules.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "博客分类")
public class BlogCategoryVO {
    @Schema(description = "分类ID") private Long id;
    @Schema(description = "名称") private String name;
    @Schema(description = "描述") private String description;
    @Schema(description = "排序") private Integer sortOrder;
    @Schema(description = "状态") private Integer status;
}
