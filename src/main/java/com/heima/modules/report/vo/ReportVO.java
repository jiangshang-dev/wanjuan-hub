package com.heima.modules.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Schema(description = "内容举报")
public class ReportVO {
    @Schema(description = "举报ID") private Long id;
    @Schema(description = "举报人ID") private Long reporterId;
    @Schema(description = "目标类型") private String targetType;
    @Schema(description = "目标ID") private Long targetId;
    @Schema(description = "举报原因") private String reason;
    @Schema(description = "补充说明") private String detail;
    @Schema(description = "处理状态") private String status;
    @Schema(description = "处理人ID") private Long handlerId;
    @Schema(description = "处理动作") private String action;
    @Schema(description = "处理说明") private String handleNote;
    @Schema(description = "处理时间") private LocalDateTime handleTime;
    @Schema(description = "创建时间") private LocalDateTime createTime;
}
