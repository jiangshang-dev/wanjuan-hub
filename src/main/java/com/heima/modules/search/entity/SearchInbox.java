package com.heima.modules.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("search_inbox")
@EqualsAndHashCode(callSuper = true)
public class SearchInbox extends BaseEntity {
    private String messageId;
    private String status;
    private String errorMessage;
    private LocalDateTime processedAt;
}
