package com.heima.modules.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.entity.BlogArticleVersion;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.blog.mapper.BlogArticleVersionMapper;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.entity.ForumEntities.TopicVersion;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicVersionMapper;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.search.entity.SearchDocument;
import com.heima.modules.search.mapper.SearchDocumentMapper;
import com.heima.modules.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchIndexServiceImpl implements SearchIndexService {
    private static final Set<String> SUPPORTED_TYPES = Set.of("BLOG", "RESOURCE", "FORUM_TOPIC");
    private static final Pattern EXTERNAL_LINK = Pattern.compile(
            "(?i)(?:https?|ftp)://[^\\s<>]+|magnet:\\?[^\\s<>]+");
    private final SearchDocumentMapper documentMapper;
    private final BlogArticleMapper articleMapper;
    private final BlogArticleVersionMapper articleVersionMapper;
    private final ResourceInfoMapper resourceInfoMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final TopicMapper topicMapper;
    private final TopicVersionMapper topicVersionMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexPublic(String contentType, Long contentId, String title, String summary,
                            String content, String url) {
        String type = contentType == null ? "" : contentType.trim().toUpperCase();
        if (!SUPPORTED_TYPES.contains(type) || contentId == null) {
            throw new ServiceException("不支持的公开索引内容");
        }
        if (url == null || !url.startsWith("/") || url.startsWith("//")) {
            throw new ServiceException("公开索引地址必须是站内相对地址");
        }
        SearchDocument document = new SearchDocument();
        document.setContentType(type);
        document.setContentId(contentId);
        document.setTitle(clean(title, 255));
        document.setSummary(clean(summary, 10000));
        document.setContent(clean(content, 1000000));
        document.setUrl(url);
        documentMapper.upsert(document);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(String contentType, Long contentId) {
        documentMapper.delete(new LambdaQueryWrapper<SearchDocument>()
                .eq(SearchDocument::getContentType, contentType)
                .eq(SearchDocument::getContentId, contentId));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronize(String contentType, Long contentId) {
        switch (contentType) {
            case "BLOG" -> synchronizeBlog(contentId);
            case "RESOURCE" -> synchronizeResource(contentId);
            case "FORUM_TOPIC" -> synchronizeTopic(contentId);
            default -> { }
        }
    }

    private void synchronizeBlog(Long articleId) {
        BlogArticle article = articleMapper.selectById(articleId);
        if (article == null || article.getDeleted() == 1 || "OFFLINE".equals(article.getStatus())
                || article.getPublishedVersionNo() == null) {
            remove("BLOG", articleId);
            return;
        }
        BlogArticleVersion version = articleVersionMapper.selectOne(new LambdaQueryWrapper<BlogArticleVersion>()
                .eq(BlogArticleVersion::getArticleId, articleId)
                .eq(BlogArticleVersion::getVersionNo, article.getPublishedVersionNo()).last("LIMIT 1"));
        if (version == null) {
            remove("BLOG", articleId);
            return;
        }
        indexPublic("BLOG", articleId, version.getTitle(), version.getSummary(),
                version.getContent(), "/blogs/" + articleId);
    }

    private void synchronizeResource(Long resourceId) {
        ResourceInfo resource = resourceInfoMapper.selectById(resourceId);
        if (resource == null || resource.getDeleted() == 1 || resource.getOnline() != 1
                || resource.getInvalid() == 1 || resource.getPublishedVersionNo() == null) {
            remove("RESOURCE", resourceId);
            return;
        }
        ResourceVersion version = resourceVersionMapper.selectOne(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resourceId)
                .eq(ResourceVersion::getVersionNo, resource.getPublishedVersionNo()).last("LIMIT 1"));
        if (version == null) {
            remove("RESOURCE", resourceId);
            return;
        }
        indexPublic("RESOURCE", resourceId, version.getTitle(), version.getSummary(),
                version.getDescription(), "/resources/" + resourceId);
    }

    private void synchronizeTopic(Long topicId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || topic.getDeleted() == 1 || "OFFLINE".equals(topic.getStatus())
                || topic.getPublishedVersionNo() == null) {
            remove("FORUM_TOPIC", topicId);
            return;
        }
        TopicVersion version = topicVersionMapper.selectOne(new LambdaQueryWrapper<TopicVersion>()
                .eq(TopicVersion::getTopicId, topicId)
                .eq(TopicVersion::getVersionNo, topic.getPublishedVersionNo()).last("LIMIT 1"));
        if (version == null) {
            remove("FORUM_TOPIC", topicId);
            return;
        }
        indexPublic("FORUM_TOPIC", topicId, version.getTitle(), "", version.getContent(),
                "/forum/topics/" + topicId);
    }

    private static String clean(String text, int maxLength) {
        if (text == null) return "";
        String cleaned = EXTERNAL_LINK.matcher(text).replaceAll("[外部链接已移除]");
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
