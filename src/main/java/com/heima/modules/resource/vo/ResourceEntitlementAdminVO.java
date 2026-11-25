package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台资源授权详情")
public class ResourceEntitlementAdminVO extends ResourceEntitlementVO {
    @Schema(description = "授权记录ID") private Long id;
    @Schema(description = "用户ID") private Long userId;
    @Schema(description = "撤销原因") private String revokeReason;
    @Schema(description = "创建时间") private LocalDateTime createTime;
    @Schema(description = "更新时间") private LocalDateTime updateTime;
}
