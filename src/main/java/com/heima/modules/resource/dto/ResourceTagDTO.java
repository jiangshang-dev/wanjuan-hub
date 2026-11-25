package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资源标签保存请求")
public class ResourceTagDTO {
    @NotBlank @Size(max = 50) @Schema(description = "标签名称") private String name;
}
