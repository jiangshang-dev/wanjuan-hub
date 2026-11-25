package com.heima.modules.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "审核明细项")
public class AuditItemVO {
    @Schema(description = "明细ID") private Long id;
    @Schema(description = "类型") private String itemType;
    @Schema(description = "审核内容或文件定位符") private String content;
    @Schema(description = "状态") private String status;
    @Schema(description = "审核建议") private String suggestion;
    @Schema(description = "原因") private String reason;
    @Schema(description = "服务商请求ID") private String providerRequestId;
}
