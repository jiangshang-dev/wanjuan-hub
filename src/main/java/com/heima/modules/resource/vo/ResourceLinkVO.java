package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "授权后可见的网盘链接")
public class ResourceLinkVO {
    @Schema(description = "链接ID") private Long id;
    @Schema(description = "网盘类型ID") private Long diskTypeId;
    @Schema(description = "网盘编码") private String diskCode;
    @Schema(description = "网盘名称") private String diskName;
    @Schema(description = "链接名称") private String linkName;
    @Schema(description = "解密后的HTTPS网盘地址") private String url;
    @Schema(description = "解密后的提取码") private String extractionCode;
}
