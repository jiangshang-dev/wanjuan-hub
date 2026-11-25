package com.heima.modules.search.service;

import com.heima.modules.search.dto.SearchIndexDocument;

import java.util.List;
import java.util.Map;

public interface SearchIndexRepository {
    String INDEX_NAME = "wanjuan_content_v1";

    void save(SearchIndexDocument document, long eventVersion);

    void delete(String contentType, Long contentId, long eventVersion);

    SearchPage search(String keyword, String contentType, int from, int size);

    record SearchPage(long total, List<SearchHit> hits) {
    }

    record SearchHit(SearchIndexDocument document, Map<String, List<String>> highlights) {
    }
}
