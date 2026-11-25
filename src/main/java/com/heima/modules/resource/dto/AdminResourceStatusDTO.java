package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "后台资源状态变更请求")
public class AdminResourceStatusDTO {
    @NotBlank @Pattern(regexp = "PUBLISHED|OFFLINE|INVALID") @Schema(description = "目标状态", allowableValues = {"PUBLISHED", "OFFLINE", "INVALID"}) private String status;
    @Size(max = 500) @Schema(description = "操作原因") private String reason;
}
