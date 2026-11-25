package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "资源历史版本，不包含链接敏感信息")
public class ResourceVersionVO {
    @Schema(description = "版本记录ID") private Long id;
    @Schema(description = "资源ID") private Long resourceId;
    @Schema(description = "版本号") private Integer versionNo;
    @Schema(description = "标题") private String title;
    @Schema(description = "摘要") private String summary;
    @Schema(description = "访问类型") private String accessType;
    @Schema(description = "积分价格") private Integer pointsPrice;
    @Schema(description = "现金价格") private BigDecimal price;
    @Schema(description = "版本状态") private String status;
    @Schema(description = "审核原因") private String auditReason;
    @Schema(description = "创建时间") private LocalDateTime createTime;
}
