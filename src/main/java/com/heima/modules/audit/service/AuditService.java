package com.heima.modules.audit.service;

import com.heima.common.PageResult;
import com.heima.modules.audit.dto.AuditMessage;
import com.heima.modules.audit.dto.AuditQueryDTO;
import com.heima.modules.audit.dto.AuditSubmitItem;
import com.heima.modules.audit.dto.ManualReviewDTO;
import com.heima.modules.audit.vo.AuditDetailVO;
import com.heima.modules.audit.vo.AuditTaskVO;

import java.util.Collection;
import java.util.List;

public interface AuditService {
    Long submit(String bizType, Long bizId, Integer versionNo, List<AuditSubmitItem> items);
    Long submit(String bizType, Long bizId, Integer version, Long authorId, String text, Collection<Long> imageFileIds);
    boolean isApproved(String bizType, Long bizId);
    void consume(AuditMessage message);
    void dispatch(Long taskId);
    int compensateOutbox();
    int compensateTimedOut();
    PageResult<AuditTaskVO> page(AuditQueryDTO query);
    AuditDetailVO detail(Long id);
    void retry(Long id);
    void review(Long id, ManualReviewDTO request, Long reviewerId);
}
