package com.heima.modules.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("file_reference")
@EqualsAndHashCode(callSuper = true)
public class FileReference extends BaseEntity {
    private Long fileId;
    private String bizType;
    private Long bizId;
    private Integer versionNo;
}
