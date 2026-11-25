package com.heima.modules.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.operation.dto.FeedbackDtos.CreateRequest;
import com.heima.modules.operation.dto.FeedbackDtos.ReplyRequest;
import com.heima.modules.operation.entity.FeedbackEntity;
import com.heima.modules.operation.mapper.FeedbackMapper;
import com.heima.modules.operation.service.FeedbackService;
import com.heima.modules.operation.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackMapper feedbackMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override @Transactional
    public String create(Long userId, CreateRequest request) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.setUserId(userId);
        entity.setContent(request.getContent().trim());
        entity.setStatus("PENDING");
        feedbackMapper.insert(entity);
        return String.valueOf(entity.getId());
    }

    @Override @Transactional(readOnly = true)
    public PageResult<FeedbackVO> userPage(Long userId, String status, long page, long size) {
        return page(new LambdaQueryWrapper<FeedbackEntity>().eq(FeedbackEntity::getUserId, userId)
                .eq(StringUtils.hasText(status), FeedbackEntity::getStatus, status)
                .orderByDesc(FeedbackEntity::getId), page, size);
    }

    @Override @Transactional(readOnly = true)
    public FeedbackVO userDetail(Long userId, Long id) {
        FeedbackEntity entity = feedbackMapper.selectOne(new LambdaQueryWrapper<FeedbackEntity>()
                .eq(FeedbackEntity::getId, id).eq(FeedbackEntity::getUserId, userId).last("LIMIT 1"));
        if (entity == null) throw new ServiceException(404, "反馈不存在");
        return feedbackVO(entity);
    }

    @Override @Transactional(readOnly = true)
    public PageResult<FeedbackVO> adminPage(String status, Long userId, String keyword, long page, long size) {
        return page(new LambdaQueryWrapper<FeedbackEntity>()
                .eq(StringUtils.hasText(status), FeedbackEntity::getStatus, status)
                .eq(userId != null, FeedbackEntity::getUserId, userId)
                .like(StringUtils.hasText(keyword), FeedbackEntity::getContent,
                        keyword == null ? "" : keyword.trim())
                .orderByDesc(FeedbackEntity::getId), page, size);
    }

    @Override @Transactional
    public void reply(Long id, ReplyRequest request, Long adminId) {
        FeedbackEntity entity = require(id);
        if ("CLOSED".equals(entity.getStatus())) throw new ServiceException(409, "已关闭的反馈不能回复");
        String reply = request.getReply().trim();
        LocalDateTime replyTime = LocalDateTime.now();
        int updated = feedbackMapper.update(null, new LambdaUpdateWrapper<FeedbackEntity>()
                .eq(FeedbackEntity::getId, id).ne(FeedbackEntity::getStatus, "CLOSED")
                .set(FeedbackEntity::getAdminReply, reply)
                .set(FeedbackEntity::getReplyAdminId, adminId)
                .set(FeedbackEntity::getReplyTime, replyTime)
                .set(FeedbackEntity::getStatus, "REPLIED"));
        if (updated == 0) throw new ServiceException(409, "反馈已关闭，请刷新后重试");
        eventPublisher.publishEvent(new UserNotificationEvent(entity.getUserId(), "SYSTEM", "意见反馈已有回复",
                reply, "OPERATION_FEEDBACK", String.valueOf(id),
                "FEEDBACK_REPLY:" + id + ":" + UUID.randomUUID()));
    }

    @Override @Transactional
    public void close(Long id) {
        FeedbackEntity entity = require(id);
        if ("CLOSED".equals(entity.getStatus())) return;
        feedbackMapper.update(null, new LambdaUpdateWrapper<FeedbackEntity>()
                .eq(FeedbackEntity::getId, id).ne(FeedbackEntity::getStatus, "CLOSED")
                .set(FeedbackEntity::getStatus, "CLOSED"));
    }

    private FeedbackEntity require(Long id) {
        FeedbackEntity entity = feedbackMapper.selectById(id);
        if (entity == null) throw new ServiceException(404, "反馈不存在");
        return entity;
    }

    private PageResult<FeedbackVO> page(LambdaQueryWrapper<FeedbackEntity> wrapper, long page, long size) {
        Page<FeedbackEntity> result = feedbackMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::feedbackVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private FeedbackVO feedbackVO(FeedbackEntity entity) {
        return new FeedbackVO(String.valueOf(entity.getId()), String.valueOf(entity.getUserId()), entity.getContent(),
                entity.getStatus(), entity.getAdminReply(), entity.getReplyTime(), entity.getCreateTime(), entity.getUpdateTime());
    }
}
