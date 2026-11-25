package com.heima.modules.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class ReportDtos {
    private ReportDtos() {}

    @Data @Schema(description = "举报请求")
    public static class CreateRequest {
        @NotBlank @Pattern(regexp = "BLOG|FORUM|FORUM_POST|COMMENT|RESOURCE") @Schema(description = "目标类型", allowableValues = {"BLOG", "FORUM", "FORUM_POST", "COMMENT", "RESOURCE"}) private String targetType;
        @NotNull @Schema(description = "目标ID") private Long targetId;
        @NotBlank @Size(max = 50) @Schema(description = "举报原因分类") private String reason;
        @Size(max = 1000) @Schema(description = "补充说明") private String detail;
    }

    @Data @Schema(description = "举报分页查询")
    public static class Query {
        @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
        @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
        @Pattern(regexp = "PENDING|PROCESSED|REJECTED") @Schema(description = "处理状态") private String status;
        @Schema(description = "目标类型") private String targetType;
    }

    @Data @Schema(description = "举报处理请求")
    public static class HandleRequest {
        @NotBlank @Pattern(regexp = "OFFLINE|IGNORE|REJECT") @Schema(description = "下线违规内容、保留内容或驳回举报", allowableValues = {"OFFLINE", "IGNORE", "REJECT"}) private String action;
        @Size(max = 1000) @Schema(description = "处理说明") private String note;
    }
}
