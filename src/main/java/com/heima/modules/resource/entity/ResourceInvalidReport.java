package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@TableName("resource_invalid_report")
@EqualsAndHashCode(callSuper = true)
public class ResourceInvalidReport extends BaseEntity {
    private Long resourceId;
    private Long linkId;
    private Long reporterId;
    private String reason;
    private String status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime handledAt;
}
