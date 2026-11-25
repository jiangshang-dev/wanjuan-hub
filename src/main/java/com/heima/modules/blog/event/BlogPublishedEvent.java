package com.heima.modules.blog.event;

public record BlogPublishedEvent(Long articleId, Integer versionNo) {
}
