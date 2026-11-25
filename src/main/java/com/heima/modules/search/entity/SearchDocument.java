package com.heima.modules.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("search_document")
@EqualsAndHashCode(callSuper = true)
public class SearchDocument extends BaseEntity {
    private String contentType;
    private Long contentId;
    private String title;
    private String summary;
    private String content;
    private String url;
}
