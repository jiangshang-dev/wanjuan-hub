package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "网盘类型")
public class ResourceDiskTypeVO {
    @Schema(description = "网盘类型ID") private Long id;
    @Schema(description = "网盘编码") private String code;
    @Schema(description = "网盘名称") private String name;
    @Schema(description = "域名白名单，仅后台返回") private String allowedHosts;
    @Schema(description = "排序值") private Integer sortOrder;
    @Schema(description = "状态") private Integer status;
}
