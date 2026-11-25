package com.heima.modules.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "后台资源授权分页查询")
public class ResourceEntitlementQueryDTO {
    @Min(1)
    @Schema(description = "页码", defaultValue = "1")
    private long page = 1;

    @Min(1)
    @Max(100)
    @Schema(description = "每页数量，最大100", defaultValue = "20")
    private long size = 20;

    @Schema(description = "用户ID，精确查询")
    private Long userId;

    @Schema(description = "资源ID，精确查询")
    private Long resourceId;

    @Size(max = 64)
    @Schema(description = "来源订单号，精确查询")
    private String sourceOrderNo;

    @Pattern(regexp = "ACTIVE|REVOKED")
    @Schema(description = "授权状态", allowableValues = {"ACTIVE", "REVOKED"})
    private String status;
}
