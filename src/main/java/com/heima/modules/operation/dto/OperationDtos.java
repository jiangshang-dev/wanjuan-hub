package com.heima.modules.operation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public final class OperationDtos {
    private OperationDtos() {}

    @Data @Schema(description = "公告保存请求")
    public static class AnnouncementRequest {
        @NotBlank @Size(max = 120) @Schema(description = "公告标题") private String title;
        @NotBlank @Size(max = 60000) @Schema(description = "公告内容") private String content;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @Schema(description = "发布开始时间，空表示立即生效") private LocalDateTime publishStart;
        @Schema(description = "发布结束时间，空表示长期有效") private LocalDateTime publishEnd;
        @NotNull @Schema(description = "排序值，越小越靠前") private Integer sortOrder = 0;
    }

    @Data @Schema(description = "推荐位保存请求")
    public static class RecommendationRequest {
        @NotBlank @Pattern(regexp = "BLOG|RESOURCE|FORUM_TOPIC") @Schema(description = "内容类型：BLOG、RESOURCE、FORUM_TOPIC") private String contentType;
        @NotNull @Positive @Schema(description = "内容ID") private Long contentId;
        @NotBlank @Size(max = 120) @Schema(description = "推荐标题") private String title;
        @Size(max = 500) @Schema(description = "推荐理由") private String reason = "";
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @NotNull @Schema(description = "排序值，越小越靠前") private Integer sortOrder = 0;
    }

    @Data @Schema(description = "广告位保存请求")
    public static class AdvertisementRequest {
        @NotBlank @Size(max = 120) @Schema(description = "广告标题") private String title;
        @NotBlank @Size(max = 500) @Schema(description = "图片地址") private String imageUrl;
        @NotBlank @Size(max = 500) @Schema(description = "跳转地址") private String targetUrl;
        @NotBlank @Size(max = 40) @Schema(description = "广告位置编码") private String position;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @Schema(description = "投放开始时间，空表示立即生效") private LocalDateTime startTime;
        @Schema(description = "投放结束时间，空表示长期有效") private LocalDateTime endTime;
        @NotNull @Schema(description = "排序值，越小越靠前") private Integer sortOrder = 0;
    }

    @Data @Schema(description = "友情链接保存请求")
    public static class FriendLinkRequest {
        @NotBlank @Size(max = 80) @Schema(description = "链接名称") private String name;
        @NotBlank @Size(max = 500) @Schema(description = "链接地址") private String url;
        @Size(max = 500) @Schema(description = "图标地址") private String logoUrl;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @NotNull @Schema(description = "排序值，越小越靠前") private Integer sortOrder = 0;
    }

    @Data @Schema(description = "热门搜索保存请求")
    public static class HotKeywordRequest {
        @NotBlank @Size(max = 100) @Schema(description = "搜索关键词") private String keyword;
        @NotNull @Min(0) @Schema(description = "搜索热度") private Long heat = 0L;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
        @NotNull @Schema(description = "排序值，越小越靠前") private Integer sortOrder = 0;
    }

    @Data @Schema(description = "网站参数保存请求")
    public static class ConfigRequest {
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._-]+") @Schema(description = "参数键") private String configKey;
        @NotNull @Size(max = 60000) @Schema(description = "参数值") private String configValue;
        @Size(max = 500) @Schema(description = "参数说明") private String description = "";
        @NotNull @Min(0) @Max(1) @Schema(description = "是否公开：0否，1是；敏感键禁止公开") private Integer publicFlag;
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
    }

    @Data @Schema(description = "启停请求")
    public static class StatusRequest {
        @NotNull @Min(0) @Max(1) @Schema(description = "状态：0停用，1启用") private Integer status;
    }
}
