package com.heima.modules.search.listener;

import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.blog.event.BlogPublishedEvent;
import com.heima.modules.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchEventListener {
    private final SearchIndexService searchIndexService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBlogPublished(BlogPublishedEvent event) {
        synchronize("BLOG", event.articleId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuditCompleted(AuditCompletedEvent event) {
        String type = switch (event.bizType()) {
            case "BLOG_ARTICLE" -> "BLOG";
            case "RESOURCE" -> "RESOURCE";
            case "FORUM_TOPIC" -> "FORUM_TOPIC";
            default -> null;
        };
        if (type != null) synchronize(type, event.bizId());
    }

    private void synchronize(String type, Long id) {
        try {
            searchIndexService.synchronize(type, id);
        } catch (Exception ex) {
            log.error("Failed to synchronize public search index, type={}, id={}", type, id, ex);
        }
    }
}
