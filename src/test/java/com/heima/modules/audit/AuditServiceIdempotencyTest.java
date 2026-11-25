package com.heima.modules.audit;

import com.heima.infrastructure.aliyun.audit.ContentAuditGateway;
import com.heima.modules.audit.dto.AuditMessage;
import com.heima.modules.audit.entity.AuditTask;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.mapper.AuditItemMapper;
import com.heima.modules.audit.mapper.AuditManualRecordMapper;
import com.heima.modules.audit.mapper.AuditOutboxMapper;
import com.heima.modules.audit.mapper.AuditTaskMapper;
import com.heima.modules.audit.mapper.MqConsumeLogMapper;
import com.heima.modules.audit.service.AuditImageResolver;
import com.heima.modules.audit.service.AuditMessageSender;
import com.heima.modules.audit.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceIdempotencyTest {
    @Mock private AuditTaskMapper taskMapper;
    @Mock private AuditItemMapper itemMapper;
    @Mock private AuditOutboxMapper outboxMapper;
    @Mock private MqConsumeLogMapper consumeLogMapper;
    @Mock private AuditManualRecordMapper manualRecordMapper;
    @Mock private ContentAuditGateway auditGateway;
    @Mock private AuditImageResolver imageResolver;
    @Mock private AuditMessageSender messageSender;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private AuditServiceImpl auditService;

    @Test
    void duplicateMessageIsAcknowledgedWithoutProcessingAgain() {
        AuditTask task = new AuditTask();
        task.setId(1L);
        task.setBizType("BLOG_ARTICLE");
        task.setBizId(8L);
        task.setVersionNo(2);
        task.setStatus(AuditStatus.PENDING.name());
        AuditMessage message = new AuditMessage("same-message", 1L, "BLOG_ARTICLE", 8L, 2);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(consumeLogMapper.insert(any(com.heima.modules.audit.entity.MqConsumeLog.class))).thenThrow(new DuplicateKeyException("duplicate message"));

        auditService.consume(message);

        verify(taskMapper, never()).update(isNull(), any());
        verifyNoInteractions(auditGateway);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
