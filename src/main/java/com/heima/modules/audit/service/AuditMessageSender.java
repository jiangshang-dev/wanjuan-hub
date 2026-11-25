package com.heima.modules.audit.service;

import com.heima.modules.audit.entity.AuditOutbox;

public interface AuditMessageSender {
    void send(AuditOutbox outbox);
}
