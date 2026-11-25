package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资源网盘链接")
public class ResourceLinkDTO {
    @NotNull @Schema(description = "网盘类型ID") private Long diskTypeId;
    @NotBlank @Size(max = 2000) @Schema(description = "HTTPS网盘地址，保存后加密存储") private String url;
    @Size(max = 64) @Schema(description = "提取码，保存后加密存储") private String extractionCode;
    @NotBlank @Size(max = 100) @Schema(description = "链接名称") private String linkName;
    @Schema(description = "排序值", defaultValue = "0") private Integer sortOrder = 0;
}
