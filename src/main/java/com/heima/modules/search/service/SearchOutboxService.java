package com.heima.modules.search.service;

import com.heima.modules.search.entity.SearchOutbox;

public interface SearchOutboxService {
    SearchOutbox record(String contentType, Long contentId);

    int compensate();
}
