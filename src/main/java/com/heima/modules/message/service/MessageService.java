package com.heima.modules.message.service;

import com.heima.common.PageResult;
import com.heima.modules.message.dto.MessageDtos.MessageVO;
import com.heima.modules.message.dto.MessageDtos.PreferenceRequest;
import com.heima.modules.message.dto.MessageDtos.PreferenceVO;
import com.heima.modules.message.dto.MessageDtos.SystemMessageRequest;

public interface MessageService {
    PageResult<MessageVO> page(Long userId, String type, Boolean read, long page, long size);
    long unreadCount(Long userId);
    void markRead(Long userId, Long messageId);
    int markAllRead(Long userId);
    PreferenceVO preferences(Long userId);
    PreferenceVO updatePreferences(Long userId, PreferenceRequest request);
    boolean notify(Long userId, String type, String title, String content,
                   String bizType, String bizId, String dedupKey);
    int sendSystemMessage(SystemMessageRequest request);
}
