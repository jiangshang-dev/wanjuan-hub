package com.heima.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "博客分页查询")
public class BlogQueryDTO {
    @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
    @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
    @Size(max = 120) @Schema(description = "标题关键词") private String keyword;
    @Schema(description = "分类ID，公开列表可用") private Long categoryId;
    @Schema(description = "状态，用户和后台列表可用") private String status;
}
