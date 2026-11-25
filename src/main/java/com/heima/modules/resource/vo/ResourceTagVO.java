package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "资源标签")
public class ResourceTagVO {
    @Schema(description = "标签ID") private Long id;
    @Schema(description = "标签名称") private String name;
}
