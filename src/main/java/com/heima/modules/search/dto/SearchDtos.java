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
        @Schema(description = "HTML转义后的标题") private String title;
        @Schema(description = "安全高亮摘要，仅包含mark标签") private String highlight;
        @Schema(description = "公开详情相对地址") private String url;
    }
}
