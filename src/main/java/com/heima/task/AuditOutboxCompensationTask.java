package com.heima.task;

import com.heima.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditOutboxCompensationTask {
    private final AuditService auditService;

    @Scheduled(fixedDelayString = "${wanjuan.audit.outbox-scan-ms:10000}")
    public void compensate() {
        auditService.compensateOutbox();
    }

    @Scheduled(fixedDelayString = "${wanjuan.audit.timeout-scan-ms:60000}")
    public void compensateTimedOut() {
        auditService.compensateTimedOut();
    }
}
