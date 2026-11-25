package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "资源失效举报")
public class ResourceInvalidReportVO {
    @Schema(description = "举报ID") private Long id;
    @Schema(description = "资源ID") private Long resourceId;
    @Schema(description = "链接ID") private Long linkId;
    @Schema(description = "举报人ID") private Long reporterId;
    @Schema(description = "失效原因") private String reason;
    @Schema(description = "处理状态") private String status;
    @Schema(description = "处理人ID") private Long handlerId;
    @Schema(description = "处理说明") private String handleNote;
    @Schema(description = "处理时间") private LocalDateTime handledAt;
    @Schema(description = "创建时间") private LocalDateTime createTime;
}
