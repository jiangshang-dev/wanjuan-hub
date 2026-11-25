package com.heima.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "博客分类保存请求")
public class BlogCategoryDTO {
    @NotBlank
    @Size(max = 50)
    @Schema(description = "分类名称")
    private String name;
    @Size(max = 255)
    @Schema(description = "描述")
    private String description;
    @Schema(description = "排序值")
    private Integer sortOrder = 0;
    @Min(0)
    @Max(1)
    @Schema(description = "1启用，0停用")
    private Integer status = 1;
}
