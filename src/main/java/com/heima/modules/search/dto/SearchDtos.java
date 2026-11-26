package com.heima.modules.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SearchDtos {
    private SearchDtos() {}

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SearchResultVO {
        @Schema(description = "公开内容类型") private String contentType;
        @Schema(description = "公开内容ID") private Long contentId;
        @Schema(description = "HTML转义后的标题，命中时仅额外包含mark标签") private String title;
        @Schema(description = "安全高亮摘要，仅包含mark标签") private String highlight;
        @Schema(description = "公开详情相对地址") private String url;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SearchCompensationVO {
        @Schema(description = "补偿事件消息ID") private String messageId;
        @Schema(description = "单调递增的索引事件版本号") private Long eventVersion;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SearchRebuildVO {
        @Schema(description = "写入重建事件的博客数量") private Integer blogCount;
        @Schema(description = "写入重建事件的论坛主题数量") private Integer topicCount;
        @Schema(description = "写入重建事件的总数量") private Integer total;
    }
}
