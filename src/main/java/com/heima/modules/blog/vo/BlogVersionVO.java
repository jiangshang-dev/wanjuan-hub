package com.heima.modules.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "博客版本记录")
public class BlogVersionVO {
    @Schema(description = "版本号") private Integer versionNo;
    @Schema(description = "标题") private String title;
    @Schema(description = "状态") private String status;
    @Schema(description = "审核原因") private String auditReason;
    @Schema(description = "创建时间") private LocalDateTime createTime;
}
