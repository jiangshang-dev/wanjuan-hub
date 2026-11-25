package com.heima.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.message.dto.MessageDtos.MessageVO;
import com.heima.modules.message.dto.MessageDtos.PreferenceRequest;
import com.heima.modules.message.dto.MessageDtos.PreferenceVO;
import com.heima.modules.message.dto.MessageDtos.SystemMessageRequest;
import com.heima.modules.message.entity.MessageEntities.Notification;
import com.heima.modules.message.entity.MessageEntities.Preference;
import com.heima.modules.message.mapper.MessageMappers.NotificationMapper;
import com.heima.modules.message.mapper.MessageMappers.PreferenceMapper;
import com.heima.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final NotificationMapper notificationMapper;
    private final PreferenceMapper preferenceMapper;
    private final UserAccountMapper userAccountMapper;

    @Override
    public PageResult<MessageVO> page(Long userId, String type, Boolean read, long page, long size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(type != null && !type.isBlank(), Notification::getType, type)
                .eq(read != null, Notification::getReadFlag, read != null && read ? 1 : 0)
                .orderByDesc(Notification::getId);
        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::messageVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getReadFlag, 0));
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long messageId) {
        Notification owned = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, messageId).eq(Notification::getUserId, userId).last("LIMIT 1"));
        if (owned == null) throw new ServiceException(404, "消息不存在");
        if (owned.getReadFlag() == 0) {
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .eq(Notification::getId, messageId).eq(Notification::getUserId, userId)
                    .eq(Notification::getReadFlag, 0)
                    .set(Notification::getReadFlag, 1).set(Notification::getReadTime, LocalDateTime.now()));
        }
    }

    @Override
    public int markAllRead(Long userId) {
        return notificationMapper.readAll(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public PreferenceVO preferences(Long userId) {
        return preferenceVO(preference(userId));
    }

    @Override
    @Transactional
    public PreferenceVO updatePreferences(Long userId, PreferenceRequest request) {
        Preference preference = preference(userId);
        preference.setAuditEnabled(flag(request.getAuditEnabled()));
        preference.setPaymentEnabled(flag(request.getPaymentEnabled()));
        preference.setSystemEnabled(flag(request.getSystemEnabled()));
        preference.setInteractionEnabled(flag(request.getInteractionEnabled()));
        preferenceMapper.updateById(preference);
        return preferenceVO(preference);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean notify(Long userId, String type, String title, String content,
                          String bizType, String bizId, String dedupKey) {
        if (userId == null || dedupKey == null || dedupKey.isBlank()) return false;
        Notification existing = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getDedupKey, dedupKey).last("LIMIT 1"));
        if (existing != null || !enabled(preference(userId), type)) return false;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setDedupKey(dedupKey);
        notification.setReadFlag(0);
        try {
            notificationMapper.insert(notification);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    @Override
    public int sendSystemMessage(SystemMessageRequest request) {
        String key = request.getDedupKey() == null || request.getDedupKey().isBlank()
                ? "SYSTEM:" + UUID.randomUUID() : "SYSTEM:" + request.getDedupKey();
        if (request.getUserId() != null) {
            return notify(request.getUserId(), "SYSTEM", request.getTitle(), request.getContent(),
                    "SYSTEM", null, key) ? 1 : 0;
        }
        int sent = 0;
        long page = 1;
        while (true) {
            Page<UserAccount> users = userAccountMapper.selectPage(new Page<>(page, 500), null);
            for (UserAccount user : users.getRecords()) {
                if (notify(user.getId(), "SYSTEM", request.getTitle(), request.getContent(),
                        "SYSTEM", null, key)) sent++;
            }
            if (users.getRecords().isEmpty() || page * users.getSize() >= users.getTotal()) break;
            page++;
        }
        return sent;
    }

    private Preference preference(Long userId) {
        preferenceMapper.insertIgnore(userId);
        return preferenceMapper.selectById(userId);
    }

    private boolean enabled(Preference preference, String type) {
        if (preference == null) return true;
        return switch (type) {
            case "AUDIT" -> preference.getAuditEnabled() == 1;
            case "PAYMENT" -> preference.getPaymentEnabled() == 1;
            case "INTERACTION" -> preference.getInteractionEnabled() == 1;
            default -> preference.getSystemEnabled() == 1;
        };
    }

    private MessageVO messageVO(Notification message) {
        return new MessageVO(message.getId(), message.getType(), message.getTitle(), message.getContent(),
                message.getBizType(), message.getBizId(), message.getReadFlag() == 1,
                message.getReadTime(), message.getCreateTime());
    }

    private PreferenceVO preferenceVO(Preference preference) {
        return new PreferenceVO(preference.getAuditEnabled() == 1, preference.getPaymentEnabled() == 1,
                preference.getSystemEnabled() == 1, preference.getInteractionEnabled() == 1);
    }

    private int flag(Boolean value) { return Boolean.TRUE.equals(value) ? 1 : 0; }
}
