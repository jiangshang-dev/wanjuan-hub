package com.heima.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@TableName("resource_link")
@EqualsAndHashCode(callSuper = true)
public class ResourceLink extends BaseEntity {
    private Long resourceId;
    private Integer versionNo;
    private Long diskTypeId;
    @ToString.Exclude
    private String urlCiphertext;
    @ToString.Exclude
    private String extractionCodeCiphertext;
    private String linkName;
    private String status;
    private Integer sortOrder;
}
