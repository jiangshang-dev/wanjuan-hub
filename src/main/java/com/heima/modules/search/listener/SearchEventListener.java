package com.heima.modules.search.listener;

import com.heima.modules.blog.event.BlogPublishedEvent;
import com.heima.modules.search.event.SearchIndexRefreshEvent;
import com.heima.modules.search.service.SearchOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SearchEventListener {
    private static final Set<String> PUBLIC_TYPES = Set.of("BLOG", "FORUM_TOPIC");
    private final SearchOutboxService searchOutboxService;

    @EventListener
    public void onBlogPublished(BlogPublishedEvent event) {
        searchOutboxService.record("BLOG", event.articleId());
    }

    @EventListener
    public void onIndexRefresh(SearchIndexRefreshEvent event) {
        if (PUBLIC_TYPES.contains(event.contentType())) {
            searchOutboxService.record(event.contentType(), event.contentId());
        }
    }
}
