package com.heima.modules.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("blog_article_version")
@EqualsAndHashCode(callSuper = true)
public class BlogArticleVersion extends BaseEntity {
    private Long articleId;
    private Integer versionNo;
    private String title;
    private String summary;
    private String content;
    private Long coverFileId;
    private String contentFileIds;
    private String status;
    private String auditReason;
}
