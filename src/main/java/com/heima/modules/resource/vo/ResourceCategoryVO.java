package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "资源分类")
public class ResourceCategoryVO {
    @Schema(description = "分类ID") private Long id;
    @Schema(description = "分类名称") private String name;
    @Schema(description = "分类描述") private String description;
    @Schema(description = "排序值") private Integer sortOrder;
    @Schema(description = "状态，公开接口仅返回启用分类") private Integer status;
}
