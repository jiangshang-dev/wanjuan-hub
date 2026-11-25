package com.heima.modules.blog.listener;

import com.heima.modules.blog.event.BlogPublishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class BlogPublishedListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublished(BlogPublishedEvent event) {
        log.info("Blog public version switched, articleId={}, version={}", event.articleId(), event.versionNo());
    }
}
