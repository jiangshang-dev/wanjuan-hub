package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资源失效举报请求")
public class ResourceInvalidReportDTO {
    @Schema(description = "失效链接ID，整资源失效时可为空") private Long linkId;
    @NotBlank @Size(max = 1000) @Schema(description = "失效原因") private String reason;
}
