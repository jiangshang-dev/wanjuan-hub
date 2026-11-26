package com.heima.modules.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.audit.enums.OutboxStatus;
import com.heima.modules.search.entity.SearchOutbox;
import com.heima.modules.search.event.SearchIndexMessage;
import com.heima.modules.search.mapper.SearchOutboxMapper;
import com.heima.modules.search.service.SearchMessageSender;
import com.heima.modules.search.service.SearchOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchOutboxServiceImpl implements SearchOutboxService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> SUPPORTED_TYPES = Set.of("BLOG", "FORUM_TOPIC");
    private final SearchOutboxMapper outboxMapper;
    private final SearchMessageSender messageSender;

    @Override
    @Transactional
    public SearchOutbox record(String contentType, Long contentId) {
        String type = contentType == null ? "" : contentType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(type) || contentId == null || contentId <= 0) {
            throw new ServiceException(400, "不支持的搜索索引内容");
        }
        SearchOutbox outbox = new SearchOutbox();
        outbox.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        outbox.setContentType(type);
        outbox.setContentId(contentId);
        outbox.setEventVersion(0L);
        outbox.setPayload("");
        outbox.setStatus(OutboxStatus.PENDING.name());
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
        outbox.setEventVersion(outbox.getId());
        outbox.setPayload(JSON.writeValueAsString(new SearchIndexMessage(
                outbox.getMessageId(), type, contentId, outbox.getEventVersion())));
        outboxMapper.update(null, new LambdaUpdateWrapper<SearchOutbox>().eq(SearchOutbox::getId, outbox.getId())
                .set(SearchOutbox::getEventVersion, outbox.getEventVersion())
                .set(SearchOutbox::getPayload, outbox.getPayload()));
        return outbox;
    }

    @Override
    public int compensate() {
        LocalDateTime now = LocalDateTime.now();
        List<SearchOutbox> candidates = outboxMapper.selectList(new LambdaQueryWrapper<SearchOutbox>()
                .in(SearchOutbox::getStatus, OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name())
                .le(SearchOutbox::getNextRetryAt, now)
                .and(wrapper -> wrapper.isNull(SearchOutbox::getLeaseUntil).or().le(SearchOutbox::getLeaseUntil, now))
                .orderByAsc(SearchOutbox::getId).last("LIMIT 100"));
        int dispatched = 0;
        for (SearchOutbox candidate : candidates) {
            String owner = UUID.randomUUID().toString();
            int claimed = outboxMapper.update(null, new LambdaUpdateWrapper<SearchOutbox>()
                    .eq(SearchOutbox::getId, candidate.getId()).eq(SearchOutbox::getMessageId, candidate.getMessageId())
                    .in(SearchOutbox::getStatus, OutboxStatus.PENDING.name(), OutboxStatus.FAILED.name())
                    .and(wrapper -> wrapper.isNull(SearchOutbox::getLeaseUntil).or().le(SearchOutbox::getLeaseUntil, now))
                    .set(SearchOutbox::getLeaseOwner, owner).set(SearchOutbox::getLeaseUntil, now.plusMinutes(1)));
            if (claimed == 0) continue;
            messageSender.send(outboxMapper.selectById(candidate.getId()));
            dispatched++;
        }
        return dispatched;
    }
}
