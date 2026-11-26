package com.heima.modules.search;

import com.heima.modules.blog.event.BlogPublishedEvent;
import com.heima.modules.search.event.SearchIndexRefreshEvent;
import com.heima.modules.search.listener.SearchEventListener;
import com.heima.modules.search.service.SearchOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchEventListenerTest {
    @Mock private SearchOutboxService outboxService;
    @InjectMocks private SearchEventListener listener;

    @Test
    void blogPublicationOnlyWritesTransactionalOutbox() {
        listener.onBlogPublished(new BlogPublishedEvent(8L, 3));

        verify(outboxService).record("BLOG", 8L);
    }

    @Test
    void resourceEventIsNotIndexed() {
        listener.onIndexRefresh(new SearchIndexRefreshEvent("RESOURCE", 6L));

        verifyNoInteractions(outboxService);
    }
}
