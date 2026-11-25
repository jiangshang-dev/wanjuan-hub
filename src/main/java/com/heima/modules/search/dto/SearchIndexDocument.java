package com.heima.modules.search.dto;

public record SearchIndexDocument(
        String contentType,
        Long contentId,
        String title,
        String summary,
        String content,
        String url) {
}
