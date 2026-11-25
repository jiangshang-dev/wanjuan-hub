package com.heima.modules.audit.event;

import com.heima.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditDispatchListener {
    private final AuditService auditService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(AuditSubmittedEvent event) {
        auditService.dispatch(event.taskId());
    }
}
