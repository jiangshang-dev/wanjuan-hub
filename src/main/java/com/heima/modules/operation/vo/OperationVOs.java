package com.heima.modules.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public final class OperationVOs {
    private OperationVOs() {}

    @Schema(description = "运营公告")
    public record AnnouncementVO(@Schema(description = "公告ID，字符串") String id,
            @Schema(description = "标题") String title, @Schema(description = "内容") String content,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "发布开始时间") LocalDateTime publishStart,
            @Schema(description = "发布结束时间") LocalDateTime publishEnd,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "推荐位")
    public record RecommendationVO(@Schema(description = "推荐位ID，字符串") String id,
            @Schema(description = "内容类型") String contentType,
            @Schema(description = "内容ID，字符串") String contentId,
            @Schema(description = "推荐标题") String title, @Schema(description = "推荐理由") String reason,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "广告位")
    public record AdvertisementVO(@Schema(description = "广告ID，字符串") String id,
            @Schema(description = "标题") String title, @Schema(description = "图片地址") String imageUrl,
            @Schema(description = "跳转地址") String targetUrl, @Schema(description = "位置编码") String position,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "投放开始时间") LocalDateTime startTime,
            @Schema(description = "投放结束时间") LocalDateTime endTime,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "友情链接")
    public record FriendLinkVO(@Schema(description = "链接ID，字符串") String id,
            @Schema(description = "名称") String name, @Schema(description = "地址") String url,
            @Schema(description = "图标地址") String logoUrl,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "热门搜索词")
    public record HotKeywordVO(@Schema(description = "关键词ID，字符串") String id,
            @Schema(description = "关键词") String keyword, @Schema(description = "热度") Long heat,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "后台网站参数")
    public record ConfigVO(@Schema(description = "参数ID，字符串") String id,
            @Schema(description = "参数键") String configKey, @Schema(description = "参数值") String configValue,
            @Schema(description = "参数说明") String description,
            @Schema(description = "是否公开：0否，1是") Integer publicFlag,
            @Schema(description = "状态：0停用，1启用") Integer status,
            @Schema(description = "创建时间") LocalDateTime createTime,
            @Schema(description = "更新时间") LocalDateTime updateTime) {}

    @Schema(description = "公开网站参数，仅包含明确允许公开且非敏感的配置")
    public record PublicConfigVO(@Schema(description = "参数键") String configKey,
            @Schema(description = "参数值") String configValue,
            @Schema(description = "参数说明") String description) {}
}
