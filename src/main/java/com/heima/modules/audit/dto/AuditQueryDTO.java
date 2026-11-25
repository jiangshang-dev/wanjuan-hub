package com.heima.modules.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "审核任务查询")
public class AuditQueryDTO {
    @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
    @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
    @Schema(description = "任务状态") private String status;
    @Schema(description = "业务类型") private String bizType;
}
