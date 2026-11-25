package com.heima.modules.blog.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.blog.constant.BlogConstants;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.entity.BlogArticleVersion;
import com.heima.modules.blog.enums.ArticleStatus;
import com.heima.modules.blog.enums.VersionStatus;
import com.heima.modules.blog.event.BlogPublishedEvent;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.blog.mapper.BlogArticleVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BlogAuditListener {
    private final BlogArticleMapper articleMapper;
    private final BlogArticleVersionMapper versionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompleted(AuditCompletedEvent event) {
        if (!BlogConstants.AUDIT_BIZ_TYPE.equals(event.bizType())) return;
        BlogArticle article = articleMapper.selectById(event.bizId());
        if (article == null || article.getDeleted() == 1) return;
        BlogArticleVersion version = versionMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BlogArticleVersion>()
                .eq(BlogArticleVersion::getArticleId, event.bizId()).eq(BlogArticleVersion::getVersionNo, event.versionNo()));
        if (version == null || !article.getLatestVersionNo().equals(event.versionNo())) {
            versionMapper.update(null, new LambdaUpdateWrapper<BlogArticleVersion>().eq(BlogArticleVersion::getArticleId, event.bizId())
                    .eq(BlogArticleVersion::getVersionNo, event.versionNo()).eq(BlogArticleVersion::getStatus, VersionStatus.PENDING.name())
                    .set(BlogArticleVersion::getStatus, VersionStatus.SUPERSEDED.name()).set(BlogArticleVersion::getAuditReason, "已有更新版本，本结果不再生效"));
            return;
        }
        if (event.status() == AuditStatus.APPROVED) {
            int versionUpdated = versionMapper.update(null, new LambdaUpdateWrapper<BlogArticleVersion>().eq(BlogArticleVersion::getId, version.getId())
                    .in(BlogArticleVersion::getStatus, VersionStatus.PENDING.name(), VersionStatus.MANUAL_REVIEW.name(), VersionStatus.REJECTED.name()).set(BlogArticleVersion::getStatus, VersionStatus.APPROVED.name())
                    .set(BlogArticleVersion::getAuditReason, null));
            if (versionUpdated == 0) return;
            int published = articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, article.getId())
                    .eq(BlogArticle::getLatestVersionNo, event.versionNo())
                    .in(BlogArticle::getStatus, ArticleStatus.PENDING.name(), ArticleStatus.MANUAL_REVIEW.name(), ArticleStatus.REJECTED.name())
                    .eq(BlogArticle::getDeleted, 0).set(BlogArticle::getPublishedVersionNo, event.versionNo())
                    .set(BlogArticle::getStatus, ArticleStatus.PUBLISHED.name()).set(BlogArticle::getAuditReason, null));
            if (published == 0) {
                versionMapper.update(null, new LambdaUpdateWrapper<BlogArticleVersion>().eq(BlogArticleVersion::getId, version.getId())
                        .set(BlogArticleVersion::getStatus, VersionStatus.SUPERSEDED.name()).set(BlogArticleVersion::getAuditReason, "文章状态已变化，未切换公开版本"));
                return;
            }
            eventPublisher.publishEvent(new BlogPublishedEvent(article.getId(), event.versionNo()));
            return;
        }
        VersionStatus versionStatus = event.status() == AuditStatus.REJECTED ? VersionStatus.REJECTED : VersionStatus.MANUAL_REVIEW;
        ArticleStatus articleStatus = event.status() == AuditStatus.REJECTED ? ArticleStatus.REJECTED : ArticleStatus.MANUAL_REVIEW;
        versionMapper.update(null, new LambdaUpdateWrapper<BlogArticleVersion>().eq(BlogArticleVersion::getId, version.getId())
                .in(BlogArticleVersion::getStatus, VersionStatus.PENDING.name(), VersionStatus.MANUAL_REVIEW.name(), VersionStatus.REJECTED.name()).set(BlogArticleVersion::getStatus, versionStatus.name())
                .set(BlogArticleVersion::getAuditReason, event.reason()));
        articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, article.getId())
                .eq(BlogArticle::getLatestVersionNo, event.versionNo())
                .in(BlogArticle::getStatus, ArticleStatus.PENDING.name(), ArticleStatus.MANUAL_REVIEW.name(), ArticleStatus.REJECTED.name())
                .set(BlogArticle::getStatus, articleStatus.name()).set(BlogArticle::getAuditReason, event.reason()));
    }
}
