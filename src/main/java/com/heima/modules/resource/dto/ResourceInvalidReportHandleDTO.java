package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资源失效举报处理请求")
public class ResourceInvalidReportHandleDTO {
    @NotBlank @Pattern(regexp = "CONFIRMED|REJECTED") @Schema(description = "处理结果", allowableValues = {"CONFIRMED", "REJECTED"}) private String status;
    @Size(max = 1000) @Schema(description = "处理说明") private String handleNote;
}
