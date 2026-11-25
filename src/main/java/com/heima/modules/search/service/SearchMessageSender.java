package com.heima.modules.search.service;

import com.heima.modules.search.entity.SearchOutbox;

public interface SearchMessageSender {
    void send(SearchOutbox outbox);
}
