package com.heima.modules.search.service.impl;

import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.search.dto.SearchDtos.SearchCompensationVO;
import com.heima.modules.search.dto.SearchDtos.SearchRebuildVO;
import com.heima.modules.search.entity.SearchOutbox;
import com.heima.modules.search.service.SearchMaintenanceService;
import com.heima.modules.search.service.SearchOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchMaintenanceServiceImpl implements SearchMaintenanceService {
    private final BlogArticleMapper articleMapper;
    private final TopicMapper topicMapper;
    private final SearchOutboxService outboxService;

    @Override
    @Transactional
    public SearchCompensationVO compensate(String contentType, Long contentId) {
        SearchOutbox outbox = outboxService.record(contentType, contentId);
        return new SearchCompensationVO(outbox.getMessageId(), outbox.getEventVersion());
    }

    @Override
    @Transactional
    public SearchRebuildVO rebuildAll() {
        List<Long> articleIds = articleMapper.selectAllIdsIncludingDeleted();
        List<Long> topicIds = topicMapper.selectAllIdsIncludingDeleted();
        articleIds.forEach(id -> outboxService.record("BLOG", id));
        topicIds.forEach(id -> outboxService.record("FORUM_TOPIC", id));
        return new SearchRebuildVO(articleIds.size(), topicIds.size(), articleIds.size() + topicIds.size());
    }
}
