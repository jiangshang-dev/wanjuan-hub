package com.heima.modules.search.event;

public record SearchIndexMessage(
        String messageId,
        String contentType,
        Long contentId,
        long eventVersion) {
}
