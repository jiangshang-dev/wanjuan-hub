package com.heima.modules.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "文件临时访问地址")
public class FileAccessVO {
    @Schema(description = "文件ID") private Long id;
    @Schema(description = "短时效预签名URL") private String url;
    @Schema(description = "过期时间") private LocalDateTime expiresAt;
}
