package com.heima.modules.blog.converter;

import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.entity.BlogArticleVersion;
import com.heima.modules.blog.vo.BlogArticleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
public class BlogConverter {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public BlogArticleVO toVO(BlogArticle article, BlogArticleVersion version, List<String> tags) {
        BlogArticleVO vo = new BlogArticleVO();
        BeanUtils.copyProperties(article, vo);
        BeanUtils.copyProperties(version, vo);
        vo.setId(article.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setContentFileIds(JSON.readValue(version.getContentFileIds(), new TypeReference<List<Long>>() {}));
        vo.setTags(tags);
        return vo;
    }

    public BlogArticleVO toPublicVO(BlogArticle article, BlogArticleVersion version, List<String> tags) {
        BlogArticleVO vo = toVO(article, version, tags);
        vo.setStatus(null);
        vo.setAuditReason(null);
        vo.setLatestVersionNo(null);
        return vo;
    }
}
