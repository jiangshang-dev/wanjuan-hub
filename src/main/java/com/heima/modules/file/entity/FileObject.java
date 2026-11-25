package com.heima.modules.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("file_object")
@EqualsAndHashCode(callSuper = true)
public class FileObject extends BaseEntity {
    private Long ownerId;
    private String fileType;
    private String bucketName;
    private String objectKey;
    private String publicBucket;
    private String publicObjectKey;
    private String originalName;
    private String extension;
    private String mimeType;
    private Long sizeBytes;
    private String sha256;
    private String visibility;
    private String auditStatus;
    private String auditReason;
}
