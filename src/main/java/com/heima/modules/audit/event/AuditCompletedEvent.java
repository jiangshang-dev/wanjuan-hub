package com.heima.modules.audit.event;

import com.heima.modules.audit.enums.AuditStatus;

public record AuditCompletedEvent(Long taskId, String bizType, Long bizId, Integer versionNo,
                                  AuditStatus status, String reason) {
}
