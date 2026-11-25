package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "资源失效举报分页查询")
public class ResourceReportQueryDTO {
    @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
    @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
    @Pattern(regexp = "PENDING|CONFIRMED|REJECTED") @Schema(description = "处理状态") private String status;
    @Schema(description = "资源ID") private Long resourceId;
}
