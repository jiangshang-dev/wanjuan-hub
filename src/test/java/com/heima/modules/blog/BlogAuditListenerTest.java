package com.heima.modules.blog;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.blog.constant.BlogConstants;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.entity.BlogArticleVersion;
import com.heima.modules.blog.enums.ArticleStatus;
import com.heima.modules.blog.enums.VersionStatus;
import com.heima.modules.blog.event.BlogPublishedEvent;
import com.heima.modules.blog.listener.BlogAuditListener;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.blog.mapper.BlogArticleVersionMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogAuditListenerTest {
    @Mock private BlogArticleMapper articleMapper;
    @Mock private BlogArticleVersionMapper versionMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private BlogAuditListener listener;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlogArticle.class);
        TableInfoHelper.initTableInfo(assistant, BlogArticleVersion.class);
    }

    @Test
    void approvedCurrentVersionAtomicallySwitchesPublicPointer() {
        BlogArticle article = article(2, ArticleStatus.PENDING);
        BlogArticleVersion version = version(2, VersionStatus.PENDING);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(versionMapper.selectOne(any())).thenReturn(version);
        when(versionMapper.update(isNull(), any())).thenReturn(1);
        when(articleMapper.update(isNull(), any())).thenReturn(1);
        listener.onCompleted(event(2, AuditStatus.APPROVED));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<BlogArticle>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(articleMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("published_version_no"));
        assertTrue(captor.getValue().getSqlSet().contains("status="));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("PUBLISHED"));
        verify(eventPublisher).publishEvent(any(BlogPublishedEvent.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = ArticleStatus.class, names = {"MANUAL_REVIEW", "REJECTED"})
    void manualApprovalPublishesCurrentVersion(ArticleStatus currentStatus) {
        BlogArticle article = article(1, currentStatus);
        BlogArticleVersion version = version(1, VersionStatus.valueOf(currentStatus.name()));
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(versionMapper.selectOne(any())).thenReturn(version);
        when(versionMapper.update(isNull(), any())).thenReturn(1);
        when(articleMapper.update(isNull(), any())).thenReturn(1);

        listener.onCompleted(event(1, AuditStatus.APPROVED));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<BlogArticleVersion>> versionCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(versionMapper).update(isNull(), versionCaptor.capture());
        versionCaptor.getValue().getSqlSegment();
        assertTrue(versionCaptor.getValue().getParamNameValuePairs().containsValue(currentStatus.name()),
                versionCaptor.getValue().getParamNameValuePairs().toString());
        assertTrue(versionCaptor.getValue().getParamNameValuePairs().containsValue("APPROVED"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<BlogArticle>> articleCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(articleMapper).update(isNull(), articleCaptor.capture());
        articleCaptor.getValue().getSqlSegment();
        assertTrue(articleCaptor.getValue().getParamNameValuePairs().containsValue(currentStatus.name()));
        assertTrue(articleCaptor.getValue().getParamNameValuePairs().containsValue("PUBLISHED"));
        verify(eventPublisher).publishEvent(any(BlogPublishedEvent.class));
    }

    @Test
    void staleApprovedResultCannotOverwriteNewerDraft() {
        BlogArticle article = article(2, ArticleStatus.DRAFT);
        BlogArticleVersion oldVersion = version(1, VersionStatus.PENDING);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(versionMapper.selectOne(any())).thenReturn(oldVersion);
        listener.onCompleted(event(1, AuditStatus.APPROVED));
        verify(articleMapper, never()).update(isNull(), any());
        verify(versionMapper).update(isNull(), any());
        verify(eventPublisher, never()).publishEvent(any(BlogPublishedEvent.class));
    }

    private AuditCompletedEvent event(int version, AuditStatus status) {
        return new AuditCompletedEvent(10L, BlogConstants.AUDIT_BIZ_TYPE, 1L, version, status, "ok");
    }

    private BlogArticle article(int latest, ArticleStatus status) {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setLatestVersionNo(latest);
        article.setPublishedVersionNo(1);
        article.setStatus(status.name());
        article.setDeleted(0);
        return article;
    }

    private BlogArticleVersion version(int number, VersionStatus status) {
        BlogArticleVersion version = new BlogArticleVersion();
        version.setId((long) number);
        version.setArticleId(1L);
        version.setVersionNo(number);
        version.setStatus(status.name());
        return version;
    }
}
