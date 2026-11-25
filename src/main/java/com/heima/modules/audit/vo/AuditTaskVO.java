package com.heima.modules.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "审核任务")
public class AuditTaskVO {
    @Schema(description = "任务ID") private Long id;
    @Schema(description = "业务类型") private String bizType;
    @Schema(description = "业务ID") private Long bizId;
    @Schema(description = "作者ID") private Long authorId;
    @Schema(description = "业务版本") private Integer versionNo;
    @Schema(description = "状态") private String status;
    @Schema(description = "原因") private String reason;
    @Schema(description = "重试次数") private Integer retryCount;
    @Schema(description = "创建时间") private LocalDateTime createTime;
    @Schema(description = "完成时间") private LocalDateTime finishedAt;
}
