package com.heima.modules.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.aliyun.audit.AuditGatewayResult;
import com.heima.infrastructure.aliyun.audit.ContentAuditGateway;
import com.heima.infrastructure.aliyun.audit.GatewayDecision;
import com.heima.modules.audit.dto.AuditMessage;
import com.heima.modules.audit.dto.AuditQueryDTO;
import com.heima.modules.audit.dto.AuditSubmitItem;
import com.heima.modules.audit.dto.ManualReviewDTO;
import com.heima.modules.audit.entity.AuditItem;
import com.heima.modules.audit.entity.AuditManualRecord;
import com.heima.modules.audit.entity.AuditOutbox;
import com.heima.modules.audit.entity.AuditTask;
import com.heima.modules.audit.entity.MqConsumeLog;
import com.heima.modules.audit.enums.AuditItemType;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.enums.OutboxStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.audit.event.AuditSubmittedEvent;
import com.heima.modules.audit.mapper.AuditItemMapper;
import com.heima.modules.audit.mapper.AuditManualRecordMapper;
import com.heima.modules.audit.mapper.AuditOutboxMapper;
import com.heima.modules.audit.mapper.AuditTaskMapper;
import com.heima.modules.audit.mapper.MqConsumeLogMapper;
import com.heima.modules.audit.service.AuditImageResolver;
import com.heima.modules.audit.service.AuditMessageSender;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.audit.vo.AuditDetailVO;
import com.heima.modules.audit.vo.AuditItemVO;
import com.heima.modules.audit.vo.AuditManualVO;
import com.heima.modules.audit.vo.AuditTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CONSUMER = "audit-task-consumer";
    private final AuditTaskMapper taskMapper;
    private final AuditItemMapper itemMapper;
    private final AuditOutboxMapper outboxMapper;
    private final MqConsumeLogMapper consumeLogMapper;
    private final AuditManualRecordMapper manualRecordMapper;
    private final ContentAuditGateway auditGateway;
    private final AuditImageResolver imageResolver;
    private final AuditMessageSender messageSender;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Long submit(String bizType, Long bizId, Integer versionNo, List<AuditSubmitItem> items) {
        return submitInternal(bizType, bizId, versionNo, null, items);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public Long submit(String bizType, Long bizId, Integer version, Long authorId, String text, java.util.Collection<Long> imageFileIds) {
        List<AuditSubmitItem> items = new ArrayList<>();
        if (text != null && !text.isBlank()) items.add(AuditSubmitItem.text(text));
        if (imageFileIds != null) new LinkedHashSet<>(imageFileIds).forEach(fileId -> items.add(AuditSubmitItem.image(fileId)));
        return submitInternal(bizType, bizId, version, authorId, items);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApproved(String bizType, Long bizId) {
        List<AuditTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<AuditTask>().eq(AuditTask::getBizType, bizType)
                .eq(AuditTask::getBizId, bizId).orderByDesc(AuditTask::getVersionNo).orderByDesc(AuditTask::getId).last("LIMIT 1"));
        return !tasks.isEmpty() && AuditStatus.APPROVED.name().equals(tasks.getFirst().getStatus());
    }

    private Long submitInternal(String bizType, Long bizId, Integer versionNo, Long authorId, List<AuditSubmitItem> items) {
        if (bizType == null || bizType.isBlank() || bizId == null || versionNo == null || versionNo < 1 || items == null || items.isEmpty()) {
            throw new ServiceException(400, "审核任务参数不完整");
        }
        AuditTask task = new AuditTask();
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setAuthorId(authorId);
        task.setVersionNo(versionNo);
        task.setStatus(AuditStatus.PENDING.name());
        task.setRetryCount(0);
        taskMapper.insert(task);
        for (AuditSubmitItem submitItem : items) {
            if (submitItem == null || submitItem.type() == null || submitItem.content() == null || submitItem.content().isBlank()) {
                throw new ServiceException(400, "审核明细不能为空");
            }
            AuditItem item = new AuditItem();
            item.setTaskId(task.getId());
            item.setItemType(submitItem.type().name());
            item.setContent(submitItem.content());
            item.setStatus(AuditStatus.PENDING.name());
            itemMapper.insert(item);
        }
        createOutbox(task);
        eventPublisher.publishEvent(new AuditSubmittedEvent(task.getId()));
        return task.getId();
    }

    @Override
    @Transactional
    public void consume(AuditMessage message) {
        AuditTask task = taskMapper.selectById(message.taskId());
        if (task == null) {
            throw new ServiceException(404, "审核任务不存在");
        }
        MqConsumeLog log = new MqConsumeLog();
        log.setMessageId(message.messageId());
        log.setConsumerName(CONSUMER);
        log.setStatus("PROCESSING");
        try {
            consumeLogMapper.insert(log);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        if (!sameVersion(task, message)) {
            finishLog(log.getId(), "IGNORED", "消息元数据与当前任务不一致");
            return;
        }
        int claimed = taskMapper.update(null, new LambdaUpdateWrapper<AuditTask>()
                .eq(AuditTask::getId, task.getId()).eq(AuditTask::getStatus, AuditStatus.PENDING.name())
                .eq(AuditTask::getBizId, message.bizId()).eq(AuditTask::getVersionNo, message.version())
                .set(AuditTask::getStatus, AuditStatus.PROCESSING.name()).set(AuditTask::getReason, null));
        if (claimed == 0) {
            finishLog(log.getId(), "IGNORED", "任务已处理或状态已变化");
            return;
        }
        List<AuditItem> items = itemMapper.selectList(new LambdaQueryWrapper<AuditItem>()
                .eq(AuditItem::getTaskId, task.getId()).orderByAsc(AuditItem::getId));
        if (items.isEmpty()) {
            throw new ServiceException(500, "审核任务缺少明细");
        }
        AuditStatus finalStatus = AuditStatus.APPROVED;
        List<String> reasons = new ArrayList<>();
        for (AuditItem item : items) {
            String content = item.getItemType().equals(AuditItemType.IMAGE.name())
                    ? imageResolver.resolveImageUrl(item.getContent()) : item.getContent();
            AuditGatewayResult result = item.getItemType().equals(AuditItemType.IMAGE.name())
                    ? auditGateway.reviewImage(content) : auditGateway.reviewText(content);
            AuditStatus itemStatus = switch (result.decision()) {
                case PASS -> AuditStatus.APPROVED;
                case REJECT -> AuditStatus.REJECTED;
                case REVIEW -> AuditStatus.MANUAL_REVIEW;
            };
            itemMapper.update(null, new LambdaUpdateWrapper<AuditItem>().eq(AuditItem::getId, item.getId())
                    .set(AuditItem::getStatus, itemStatus.name()).set(AuditItem::getSuggestion, result.decision().name())
                    .set(AuditItem::getReason, abbreviate(result.reason())).set(AuditItem::getProviderRequestId, result.requestId()));
            if (itemStatus == AuditStatus.REJECTED) {
                finalStatus = AuditStatus.REJECTED;
            } else if (itemStatus == AuditStatus.MANUAL_REVIEW && finalStatus != AuditStatus.REJECTED) {
                finalStatus = AuditStatus.MANUAL_REVIEW;
            }
            if (result.reason() != null && itemStatus != AuditStatus.APPROVED) {
                reasons.add(result.reason());
            }
        }
        complete(task, finalStatus, reasons.isEmpty() ? defaultReason(finalStatus) : abbreviate(String.join("；", reasons)));
        finishLog(log.getId(), "SUCCESS", null);
    }

    @Override
    public void dispatch(Long taskId) {
        AuditOutbox outbox = outboxMapper.selectOne(new LambdaQueryWrapper<AuditOutbox>().eq(AuditOutbox::getTaskId, taskId));
        if (outbox == null || OutboxStatus.SENT.name().equals(outbox.getStatus())) {
            return;
        }
        messageSender.send(outbox);
    }

    @Override
    public int compensateOutbox() {
        LocalDateTime now = LocalDateTime.now();
        List<AuditOutbox> candidates = outboxMapper.selectList(new LambdaQueryWrapper<AuditOutbox>()
                .in(AuditOutbox::getStatus, OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name())
                .le(AuditOutbox::getNextRetryAt, now)
                .and(wrapper -> wrapper.isNull(AuditOutbox::getLeaseUntil).or().le(AuditOutbox::getLeaseUntil, now))
                .orderByAsc(AuditOutbox::getId).last("LIMIT 100"));
        int dispatched = 0;
        for (AuditOutbox candidate : candidates) {
            String owner = UUID.randomUUID().toString();
            int claimed = outboxMapper.update(null, new LambdaUpdateWrapper<AuditOutbox>()
                    .eq(AuditOutbox::getId, candidate.getId()).eq(AuditOutbox::getMessageId, candidate.getMessageId())
                    .in(AuditOutbox::getStatus, OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name())
                    .and(wrapper -> wrapper.isNull(AuditOutbox::getLeaseUntil).or().le(AuditOutbox::getLeaseUntil, now))
                    .set(AuditOutbox::getLeaseOwner, owner).set(AuditOutbox::getLeaseUntil, now.plusMinutes(1)));
            if (claimed == 0) {
                continue;
            }
            AuditOutbox leased = outboxMapper.selectById(candidate.getId());
            messageSender.send(leased);
            dispatched++;
        }
        return dispatched;
    }

    @Override
    @Transactional
    public int compensateTimedOut() {
        List<AuditTask> candidates = taskMapper.selectList(new LambdaQueryWrapper<AuditTask>()
                .eq(AuditTask::getStatus, AuditStatus.PROCESSING.name())
                .lt(AuditTask::getUpdateTime, LocalDateTime.now().minusMinutes(5))
                .orderByAsc(AuditTask::getId).last("LIMIT 100"));
        int completed = 0;
        for (AuditTask task : candidates) {
            try {
                complete(task, AuditStatus.FAILED, "审核处理超时，请重试");
                completed++;
            } catch (ServiceException ignored) {
                // Another consumer completed the task while compensation was scanning.
            }
        }
        return completed;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditTaskVO> page(AuditQueryDTO query) {
        long pageNo = Math.max(1, query.getPage());
        long size = Math.min(100, Math.max(1, query.getSize()));
        LambdaQueryWrapper<AuditTask> wrapper = new LambdaQueryWrapper<AuditTask>()
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), AuditTask::getStatus, query.getStatus())
                .eq(query.getBizType() != null && !query.getBizType().isBlank(), AuditTask::getBizType, query.getBizType())
                .orderByDesc(AuditTask::getId);
        Page<AuditTask> page = taskMapper.selectPage(new Page<>(pageNo, size), wrapper);
        List<AuditTaskVO> records = page.getRecords().stream().map(this::toTaskVO).toList();
        return PageResult.of(records, page.getTotal(), pageNo, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditDetailVO detail(Long id) {
        AuditTask task = requiredTask(id);
        AuditDetailVO detail = new AuditDetailVO();
        detail.setTask(toTaskVO(task));
        detail.setItems(itemMapper.selectList(new LambdaQueryWrapper<AuditItem>().eq(AuditItem::getTaskId, id).orderByAsc(AuditItem::getId))
                .stream().map(item -> {
                    AuditItemVO vo = new AuditItemVO();
                    BeanUtils.copyProperties(item, vo);
                    return vo;
                }).toList());
        detail.setManualRecords(manualRecordMapper.selectList(new LambdaQueryWrapper<AuditManualRecord>()
                        .eq(AuditManualRecord::getTaskId, id).orderByDesc(AuditManualRecord::getId)).stream().map(record -> {
                    AuditManualVO vo = new AuditManualVO();
                    BeanUtils.copyProperties(record, vo);
                    return vo;
                }).toList());
        return detail;
    }

    @Override
    @Transactional
    public void retry(Long id) {
        AuditTask task = requiredTask(id);
        if (!List.of(AuditStatus.FAILED.name(), AuditStatus.REJECTED.name(), AuditStatus.MANUAL_REVIEW.name(), AuditStatus.PENDING.name()).contains(task.getStatus())) {
            throw new ServiceException(409, "当前状态不允许重试");
        }
        taskMapper.update(null, new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId, id).eq(AuditTask::getStatus, task.getStatus())
                .set(AuditTask::getStatus, AuditStatus.PENDING.name()).set(AuditTask::getReason, "等待重试")
                .set(AuditTask::getFinishedAt, null).setSql("retry_count = retry_count + 1"));
        itemMapper.update(null, new LambdaUpdateWrapper<AuditItem>().eq(AuditItem::getTaskId, id)
                .set(AuditItem::getStatus, AuditStatus.PENDING.name()).set(AuditItem::getSuggestion, null)
                .set(AuditItem::getReason, null).set(AuditItem::getProviderRequestId, null));
        AuditOutbox outbox = outboxMapper.selectOne(new LambdaQueryWrapper<AuditOutbox>().eq(AuditOutbox::getTaskId, id));
        if (outbox == null) {
            createOutbox(task);
        } else {
            String messageId = newMessageId();
            outboxMapper.update(null, new LambdaUpdateWrapper<AuditOutbox>().eq(AuditOutbox::getId, outbox.getId())
                    .set(AuditOutbox::getMessageId, messageId).set(AuditOutbox::getPayload, payload(task, messageId))
                    .set(AuditOutbox::getStatus, OutboxStatus.PENDING.name()).set(AuditOutbox::getRetryCount, 0)
                    .set(AuditOutbox::getNextRetryAt, LocalDateTime.now()).set(AuditOutbox::getSentAt, null)
                    .set(AuditOutbox::getLeaseOwner, null).set(AuditOutbox::getLeaseUntil, null).set(AuditOutbox::getLastError, null));
        }
        eventPublisher.publishEvent(new AuditSubmittedEvent(id));
    }

    @Override
    @Transactional
    public void review(Long id, ManualReviewDTO request, Long reviewerId) {
        AuditTask task = requiredTask(id);
        AuditStatus decision = "APPROVE".equals(request.getAction()) ? AuditStatus.APPROVED : AuditStatus.REJECTED;
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId, id)
                .eq(AuditTask::getBizId, task.getBizId()).eq(AuditTask::getVersionNo, task.getVersionNo())
                .in(AuditTask::getStatus, AuditStatus.MANUAL_REVIEW.name(), AuditStatus.FAILED.name(), AuditStatus.REJECTED.name())
                .set(AuditTask::getStatus, decision.name()).set(AuditTask::getReason, abbreviate(request.getOpinion()))
                .set(AuditTask::getFinishedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new ServiceException(409, "任务状态已变化，请刷新后重试");
        }
        AuditManualRecord record = new AuditManualRecord();
        record.setTaskId(id);
        record.setReviewerId(reviewerId);
        record.setDecision(decision.name());
        record.setReason(request.getOpinion());
        manualRecordMapper.insert(record);
        eventPublisher.publishEvent(new AuditCompletedEvent(task.getId(), task.getBizType(), task.getBizId(), task.getVersionNo(), decision, request.getOpinion()));
    }

    private void complete(AuditTask task, AuditStatus status, String reason) {
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId, task.getId())
                .eq(AuditTask::getStatus, AuditStatus.PROCESSING.name()).eq(AuditTask::getBizId, task.getBizId())
                .eq(AuditTask::getVersionNo, task.getVersionNo()).set(AuditTask::getStatus, status.name())
                .set(AuditTask::getReason, abbreviate(reason)).set(AuditTask::getFinishedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new ServiceException(409, "审核任务已被其他请求变更");
        }
        eventPublisher.publishEvent(new AuditCompletedEvent(task.getId(), task.getBizType(), task.getBizId(), task.getVersionNo(), status, reason));
    }

    private void createOutbox(AuditTask task) {
        String messageId = newMessageId();
        AuditOutbox outbox = new AuditOutbox();
        outbox.setMessageId(messageId);
        outbox.setTaskId(task.getId());
        outbox.setPayload(payload(task, messageId));
        outbox.setStatus(OutboxStatus.PENDING.name());
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
    }

    private String payload(AuditTask task, String messageId) {
        return JSON.writeValueAsString(new AuditMessage(messageId, task.getId(), task.getBizType(), task.getBizId(), task.getVersionNo()));
    }

    private boolean sameVersion(AuditTask task, AuditMessage message) {
        return task.getBizType().equals(message.bizType()) && task.getBizId().equals(message.bizId())
                && task.getVersionNo().equals(message.version()) && task.getId().equals(message.taskId());
    }

    private AuditTask requiredTask(Long id) {
        AuditTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new ServiceException(404, "审核任务不存在");
        }
        return task;
    }

    private AuditTaskVO toTaskVO(AuditTask task) {
        AuditTaskVO vo = new AuditTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    private void finishLog(Long id, String status, String error) {
        consumeLogMapper.update(null, new LambdaUpdateWrapper<MqConsumeLog>().eq(MqConsumeLog::getId, id)
                .set(MqConsumeLog::getStatus, status).set(MqConsumeLog::getErrorMessage, abbreviate(error))
                .set(MqConsumeLog::getProcessedAt, LocalDateTime.now()));
    }

    private String newMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String defaultReason(AuditStatus status) {
        return status == AuditStatus.APPROVED ? "全部审核项通过" : status == AuditStatus.REJECTED ? "存在明确违规内容" : "存在疑似违规内容，需人工审核";
    }

    private String abbreviate(String value) {
        if (value == null) return null;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
