package com.heima.modules.search.event;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "公开搜索索引刷新事件")
public record SearchIndexRefreshEvent(
        @Schema(description = "内容类型：BLOG、FORUM_TOPIC；其他类型忽略") String contentType,
        @Schema(description = "内容ID") Long contentId) {
}
