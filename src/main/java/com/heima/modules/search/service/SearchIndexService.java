package com.heima.modules.search.service;

import com.heima.modules.search.event.SearchIndexMessage;

public interface SearchIndexService {
    void consume(SearchIndexMessage message);

    void synchronize(String contentType, Long contentId, long eventVersion);
}
