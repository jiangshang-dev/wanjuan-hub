package com.heima.modules.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("search_outbox")
@EqualsAndHashCode(callSuper = true)
public class SearchOutbox extends BaseEntity {
    private String messageId;
    private String contentType;
    private Long contentId;
    private Long eventVersion;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private LocalDateTime sentAt;
    private String lastError;
}
