package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@TableName("resource_version")
@EqualsAndHashCode(callSuper = true)
public class ResourceVersion extends BaseEntity {
    private Long resourceId;
    private Integer versionNo;
    private String title;
    private String summary;
    private String description;
    private Long coverFileId;
    private String attachmentFileIds;
    private String accessType;
    private Integer pointsPrice;
    private BigDecimal price;
    private String copyrightNotice;
    private String status;
    private String auditReason;
}
