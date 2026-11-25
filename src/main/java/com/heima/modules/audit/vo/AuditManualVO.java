package com.heima.modules.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "人工审核记录")
public class AuditManualVO {
    @Schema(description = "审核人ID") private Long reviewerId;
    @Schema(description = "决定") private String decision;
    @Schema(description = "原因") private String reason;
    @Schema(description = "审核时间") private LocalDateTime createTime;
}
