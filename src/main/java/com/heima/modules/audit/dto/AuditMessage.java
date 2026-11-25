package com.heima.modules.audit.dto;

public record AuditMessage(String messageId, Long taskId, String bizType, Long bizId, Integer version) {
}
