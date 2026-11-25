package com.heima.modules.points.listener;

import com.heima.modules.audit.entity.AuditTask;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.audit.mapper.AuditTaskMapper;
import com.heima.modules.points.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsAuditListener {
    private final AuditTaskMapper auditTaskMapper;
    private final PointsService pointsService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(AuditCompletedEvent event) {
        if (event.status() != AuditStatus.APPROVED) return;
        try {
            AuditTask task = auditTaskMapper.selectById(event.taskId());
            if (task == null || task.getAuthorId() == null) return;
            pointsService.rewardApproved(task.getAuthorId(), event.bizType(), event.bizId(), event.taskId());
        } catch (Exception ex) {
            log.error("Failed to reward approved audit, taskId={}", event.taskId(), ex);
        }
    }
}
