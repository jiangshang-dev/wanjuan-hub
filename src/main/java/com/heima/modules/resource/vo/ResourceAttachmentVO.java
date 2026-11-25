package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "授权后可见的附件临时地址")
public class ResourceAttachmentVO {
    @Schema(description = "文件ID") private Long id;
    @Schema(description = "临时下载地址") private String url;
    @Schema(description = "过期时间") private LocalDateTime expiresAt;
}
