package com.heima.modules.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "博客文章")
public class BlogArticleVO {
    @Schema(description = "文章ID") private Long id;
    @Schema(description = "作者ID") private Long authorId;
    @Schema(description = "分类ID") private Long categoryId;
    @Schema(description = "标题") private String title;
    @Schema(description = "摘要") private String summary;
    @Schema(description = "正文") private String content;
    @Schema(description = "封面文件ID") private Long coverFileId;
    @Schema(description = "正文图片ID") private List<Long> contentFileIds;
    @Schema(description = "标签ID") private List<Long> tagIds;
    @Schema(description = "标签") private List<String> tags;
    @Schema(description = "原创或转载") private String originalType;
    @Schema(description = "来源名称") private String sourceName;
    @Schema(description = "来源地址") private String sourceUrl;
    @Schema(description = "当前工作流状态，公开接口不返回") private String status;
    @Schema(description = "当前审核原因，仅作者和后台可见") private String auditReason;
    @Schema(description = "最新版本号") private Integer latestVersionNo;
    @Schema(description = "公开版本号") private Integer publishedVersionNo;
    @Schema(description = "本内容版本号") private Integer versionNo;
    @Schema(description = "浏览量") private Long viewCount;
    @Schema(description = "创建时间") private LocalDateTime createTime;
    @Schema(description = "更新时间") private LocalDateTime updateTime;
}
