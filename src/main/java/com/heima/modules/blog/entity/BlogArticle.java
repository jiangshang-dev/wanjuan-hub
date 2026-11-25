package com.heima.modules.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("blog_article")
@EqualsAndHashCode(callSuper = true)
public class BlogArticle extends BaseEntity {
    private Long authorId;
    private Long categoryId;
    private String originalType;
    private String sourceName;
    private String sourceUrl;
    private String status;
    private Integer latestVersionNo;
    private Integer publishedVersionNo;
    private String auditReason;
    private Long viewCount;
    private Integer deleted;
}
