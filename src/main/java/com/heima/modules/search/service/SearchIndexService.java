package com.heima.modules.search.service;

public interface SearchIndexService {
    void indexPublic(String contentType, Long contentId, String title, String summary,
                     String content, String url);
    void remove(String contentType, Long contentId);
    void synchronize(String contentType, Long contentId);
}
