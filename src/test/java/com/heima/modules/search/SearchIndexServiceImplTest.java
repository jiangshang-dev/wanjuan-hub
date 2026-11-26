package com.heima.modules.search;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.heima.modules.search.service.impl.ElasticsearchSearchIndexRepository;
import com.heima.modules.search.service.impl.SearchIndexServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexServiceImplTest {
    @Mock private SearchIndexRepository indexRepository;
    @Mock private SearchInboxMapper inboxMapper;
    @Mock private BlogArticleMapper articleMapper;
    @Mock private BlogArticleVersionMapper articleVersionMapper;
    @Mock private TopicMapper topicMapper;
    @Mock private TopicVersionMapper topicVersionMapper;
    @InjectMocks private SearchIndexServiceImpl indexService;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlogArticle.class);
        TableInfoHelper.initTableInfo(assistant, BlogArticleVersion.class);
        TableInfoHelper.initTableInfo(assistant, Topic.class);
        TableInfoHelper.initTableInfo(assistant, TopicVersion.class);
        TableInfoHelper.initTableInfo(assistant, SearchInbox.class);
    }

    @Test
    void duplicateMessageDoesNotReadMysqlOrWriteElasticsearch() {
        when(inboxMapper.insert(any(SearchInbox.class))).thenThrow(new DuplicateKeyException("duplicate"));

        indexService.consume(new SearchIndexMessage("same-message", "BLOG", 8L, 19L));

        verifyNoInteractions(indexRepository, articleMapper, articleVersionMapper, topicMapper, topicVersionMapper);
        verify(inboxMapper, never()).update(any(), any());
    }

    @Test
    void stalePublicEventReadsDeletedStateAndCannotRestoreDocument() {
        BlogArticle deleted = new BlogArticle();
        deleted.setId(8L);
        deleted.setDeleted(1);
        when(articleMapper.selectById(8L)).thenReturn(deleted);

        indexService.synchronize("BLOG", 8L, 19L);

        verify(indexRepository).delete("BLOG", 8L, 19L);
        verify(indexRepository, never()).save(any(), anyLong());
        verifyNoInteractions(articleVersionMapper);
    }

    @Test
    void currentPublicBlogIsIndexedWithEventVersionAndCleanedContent() {
        BlogArticle article = new BlogArticle();
        article.setId(8L);
        article.setDeleted(0);
        article.setStatus("DRAFT");
        article.setPublishedVersionNo(3);
        BlogArticleVersion version = new BlogArticleVersion();
        version.setTitle("公开标题");
        version.setSummary("摘要 https://example.com/private");
        version.setContent("正文");
        when(articleMapper.selectById(8L)).thenReturn(article);
        when(articleVersionMapper.selectOne(any())).thenReturn(version);

        indexService.synchronize("BLOG", 8L, 23L);

        ArgumentCaptor<SearchIndexDocument> captor = ArgumentCaptor.forClass(SearchIndexDocument.class);
        verify(indexRepository).save(captor.capture(), org.mockito.ArgumentMatchers.eq(23L));
        SearchIndexDocument document = captor.getValue();
        assertEquals("BLOG:8", ElasticsearchSearchIndexRepository.documentId(document.contentType(), document.contentId()));
        assertEquals("摘要 [外部链接已移除]", document.summary());
        assertEquals("/blogs/8", document.url());
        assertFalse(document.summary().contains("example.com"));
    }

    @Test
    void offlineTopicIsDeletedInsteadOfIndexed() {
        Topic topic = new Topic();
        topic.setId(4L);
        topic.setDeleted(0);
        topic.setStatus("OFFLINE");
        topic.setPublishedVersionNo(2);
        when(topicMapper.selectById(4L)).thenReturn(topic);

        indexService.synchronize("FORUM_TOPIC", 4L, 31L);

        verify(indexRepository).delete("FORUM_TOPIC", 4L, 31L);
        verify(indexRepository, never()).save(any(), anyLong());
        verifyNoInteractions(topicVersionMapper);
    }
}
