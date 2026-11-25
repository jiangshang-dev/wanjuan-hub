package com.heima.modules.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图片上传结果")
public class FileUploadVO {
    @Schema(description = "文件ID") private Long id;
    @Schema(description = "私有短时效访问地址，不代表已公开") private String accessUrl;
    @Schema(description = "审核状态") private String auditStatus;
    @Schema(description = "原始文件名") private String originalName;
    @Schema(description = "MIME类型") private String mimeType;
    @Schema(description = "字节数") private Long sizeBytes;
    @Schema(description = "SHA-256") private String sha256;
}
