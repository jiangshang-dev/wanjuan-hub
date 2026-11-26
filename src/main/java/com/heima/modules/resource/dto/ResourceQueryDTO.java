package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资源分页查询")
public class ResourceQueryDTO {
    @Min(1) @Schema(description = "页码", defaultValue = "1") private long page = 1;
    @Min(1) @Max(100) @Schema(description = "每页数量，最大100", defaultValue = "20") private long size = 20;
    @Size(max = 120) @Schema(description = "标题、摘要或描述关键词") private String keyword;
    @Schema(description = "分类ID") private Long categoryId;
    @Schema(description = "标签ID，公开列表可用") private Long tagId;
    @Pattern(regexp = "FREE|POINTS|MEMBER|GUEST_FREE|LOGIN_FREE|REPLY_REQUIRED|PAID") @Schema(description = "访问类型，公开列表可用；新数据统一为FREE/POINTS/MEMBER") private String accessType;
    @Pattern(regexp = "DRAFT|PENDING|PUBLISHED|OFFLINE|REJECTED|MANUAL_REVIEW|INVALID") @Schema(description = "状态，用户和后台列表可用") private String status;
}
