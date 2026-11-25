package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "资源访问权益")
public class ResourceEntitlementVO {
    @Schema(description = "资源ID") private Long resourceId;
    @Schema(description = "权益来源") private String sourceType;
    @Schema(description = "来源订单号") private String sourceOrderNo;
    @Schema(description = "支付金额") private BigDecimal amount;
    @Schema(description = "消耗积分") private Integer pointsAmount;
    @Schema(description = "权益状态") private String status;
    @Schema(description = "授权时间") private LocalDateTime grantedAt;
    @Schema(description = "撤销时间") private LocalDateTime revokedAt;
}
