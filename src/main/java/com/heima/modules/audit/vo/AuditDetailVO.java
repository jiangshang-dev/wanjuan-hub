package com.heima.modules.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "审核任务详情")
public class AuditDetailVO {
    @Schema(description = "任务") private AuditTaskVO task;
    @Schema(description = "逐项审核结果") private List<AuditItemVO> items;
    @Schema(description = "人工审核记录") private List<AuditManualVO> manualRecords;
}
