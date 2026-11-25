package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "网盘类型保存请求")
public class ResourceDiskTypeDTO {
    @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,32}") @Schema(description = "网盘编码") private String code;
    @NotBlank @Size(max = 50) @Schema(description = "网盘名称") private String name;
    @NotBlank @Size(max = 2000) @Schema(description = "逗号分隔的域名白名单，不含协议和路径") private String allowedHosts;
    @NotNull @Schema(description = "排序值") private Integer sortOrder = 0;
    @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status = 1;
}
