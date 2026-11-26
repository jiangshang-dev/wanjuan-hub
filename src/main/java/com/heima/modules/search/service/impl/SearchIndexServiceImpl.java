package com.heima.modules.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.entity.BlogArticleVersion;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.blog.mapper.BlogArticleVersionMapper;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.entity.ForumEntities.TopicVersion;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicVersionMapper;
import com.heima.modules.search.dto.SearchIndexDocument;
import com.heima.modules.search.entity.SearchInbox;
import com.heima.modules.search.event.SearchIndexMessage;
import com.heima.modules.search.mapper.SearchInboxMapper;
import com.heima.modules.search.service.SearchIndexRepository;
import com.heima.modules.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchIndexServiceImpl implements SearchIndexService {
    private static final Pattern EXTERNAL_LINK = Pattern.compile(
            "(?i)(?:https?|ftp)://[^\\s<>]+|magnet:\\?[^\\s<>]+");
    private final SearchIndexRepository indexRepository;
    private final SearchInboxMapper inboxMapper;
    private final BlogArticleMapper articleMapper;
    private final BlogArticleVersionMapper articleVersionMapper;
    private final TopicMapper topicMapper;
    private final TopicVersionMapper topicVersionMapper;

    @Override
    @Transactional
    public void consume(SearchIndexMessage message) {
        if (message == null || message.messageId() == null || message.messageId().isBlank()
                || message.contentId() == null || message.eventVersion() <= 0) {
            throw new ServiceException(400, "搜索索引消息参数不完整");
        }
        SearchInbox inbox = new SearchInbox();
        inbox.setMessageId(message.messageId());
        inbox.setStatus("PROCESSING");
        try {
            inboxMapper.insert(inbox);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        synchronize(message.contentType(), message.contentId(), message.eventVersion());
        inboxMapper.update(null, new LambdaUpdateWrapper<SearchInbox>().eq(SearchInbox::getId, inbox.getId())
                .set(SearchInbox::getStatus, "SUCCESS").set(SearchInbox::getProcessedAt, LocalDateTime.now()));
    }

    @Override
    public void synchronize(String contentType, Long contentId, long eventVersion) {
        switch (contentType) {
            case "BLOG" -> synchronizeBlog(contentId, eventVersion);
            case "FORUM_TOPIC" -> synchronizeTopic(contentId, eventVersion);
            default -> throw new ServiceException(400, "不支持的搜索索引内容");
        }
    }

    private void synchronizeBlog(Long articleId, long eventVersion) {
        BlogArticle article = articleMapper.selectById(articleId);
        if (article == null || article.getDeleted() == 1 || "OFFLINE".equals(article.getStatus())
                || article.getPublishedVersionNo() == null) {
            indexRepository.delete("BLOG", articleId, eventVersion);
            return;
        }
        BlogArticleVersion version = articleVersionMapper.selectOne(new LambdaQueryWrapper<BlogArticleVersion>()
                .eq(BlogArticleVersion::getArticleId, articleId)
                .eq(BlogArticleVersion::getVersionNo, article.getPublishedVersionNo()).last("LIMIT 1"));
        if (version == null) {
            indexRepository.delete("BLOG", articleId, eventVersion);
            return;
        }
        indexRepository.save(new SearchIndexDocument("BLOG", articleId, clean(version.getTitle(), 255),
                clean(version.getSummary(), 10000), clean(version.getContent(), 1000000),
                "/blogs/" + articleId), eventVersion);
    }

    private void synchronizeTopic(Long topicId, long eventVersion) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || topic.getDeleted() == 1 || "OFFLINE".equals(topic.getStatus())
                || topic.getPublishedVersionNo() == null) {
            indexRepository.delete("FORUM_TOPIC", topicId, eventVersion);
            return;
        }
        TopicVersion version = topicVersionMapper.selectOne(new LambdaQueryWrapper<TopicVersion>()
                .eq(TopicVersion::getTopicId, topicId)
                .eq(TopicVersion::getVersionNo, topic.getPublishedVersionNo()).last("LIMIT 1"));
        if (version == null) {
            indexRepository.delete("FORUM_TOPIC", topicId, eventVersion);
            return;
        }
        indexRepository.save(new SearchIndexDocument("FORUM_TOPIC", topicId, clean(version.getTitle(), 255),
                "", clean(version.getContent(), 1000000), "/forum/topics/" + topicId), eventVersion);
    }

    private static String clean(String text, int maxLength) {
        if (text == null) return "";
        String cleaned = EXTERNAL_LINK.matcher(text).replaceAll("[外部链接已移除]");
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
