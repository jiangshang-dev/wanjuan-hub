package com.heima.modules.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.audit.dto.AuditSubmitItem;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.blog.constant.BlogConstants;
import com.heima.modules.blog.converter.BlogConverter;
import com.heima.modules.blog.dto.AdminBlogStatusDTO;
import com.heima.modules.blog.dto.BlogCategoryDTO;
import com.heima.modules.blog.dto.BlogQueryDTO;
import com.heima.modules.blog.dto.BlogSaveDTO;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.entity.BlogArticleTag;
import com.heima.modules.blog.entity.BlogArticleVersion;
import com.heima.modules.blog.entity.BlogCategory;
import com.heima.modules.blog.entity.BlogTag;
import com.heima.modules.blog.enums.ArticleStatus;
import com.heima.modules.blog.enums.VersionStatus;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.blog.mapper.BlogArticleTagMapper;
import com.heima.modules.blog.mapper.BlogArticleVersionMapper;
import com.heima.modules.blog.mapper.BlogCategoryMapper;
import com.heima.modules.blog.mapper.BlogTagMapper;
import com.heima.modules.blog.service.BlogService;
import com.heima.modules.blog.vo.BlogArticleVO;
import com.heima.modules.blog.vo.BlogCategoryVO;
import com.heima.modules.blog.vo.BlogVersionVO;
import com.heima.modules.file.service.FileService;
import com.heima.modules.search.event.SearchIndexRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern DANGEROUS_HTML = Pattern.compile(
            "<\\s*(script|iframe|object|embed|svg|math)\\b|on[a-z]+\\s*=|javascript\\s*:|srcdoc\\s*=|data\\s*:\\s*text/html",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final BlogArticleMapper articleMapper;
    private final BlogArticleVersionMapper versionMapper;
    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;
    private final BlogArticleTagMapper articleTagMapper;
    private final BlogConverter converter;
    private final FileService fileService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogArticleVO> publicPage(BlogQueryDTO query) {
        Page<BlogArticle> page = articleMapper.selectPublicPage(new Page<>(pageNo(query), pageSize(query)), query.getCategoryId(), cleanKeyword(query.getKeyword()));
        return PageResult.of(page.getRecords().stream().map(article -> toVO(article, version(article.getId(), article.getPublishedVersionNo()), true)).toList(),
                page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public BlogArticleVO publicDetail(Long id) {
        BlogArticle article = articleMapper.selectOne(new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getId, id)
                .eq(BlogArticle::getDeleted, 0).ne(BlogArticle::getStatus, ArticleStatus.OFFLINE.name()).isNotNull(BlogArticle::getPublishedVersionNo));
        if (article == null) throw new ServiceException(404, "文章不存在或未公开");
        BlogArticleVO vo = toVO(article, version(id, article.getPublishedVersionNo()), true);
        articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, id).setSql("view_count = view_count + 1"));
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogCategoryVO> publicCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>().eq(BlogCategory::getStatus, 1)
                        .orderByAsc(BlogCategory::getSortOrder).orderByAsc(BlogCategory::getId).last("LIMIT 100"))
                .stream().map(this::categoryVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogArticleVO> userPage(BlogQueryDTO query, Long userId) {
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getAuthorId, userId)
                .eq(BlogArticle::getDeleted, 0)
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), BlogArticle::getStatus, query.getStatus()).orderByDesc(BlogArticle::getId);
        Page<BlogArticle> page = articleMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), wrapper);
        return PageResult.of(page.getRecords().stream().map(article -> toVO(article, version(article.getId(), article.getLatestVersionNo()), false)).toList(),
                page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional(readOnly = true)
    public BlogArticleVO userDetail(Long id, Long userId) {
        BlogArticle article = owned(id, userId);
        return toVO(article, version(id, article.getLatestVersionNo()), false);
    }

    @Override
    @Transactional
    public Long create(BlogSaveDTO request, Long userId) {
        validate(request, userId);
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(request, article);
        article.setAuthorId(userId);
        article.setOriginalType(request.getType());
        article.setStatus(ArticleStatus.DRAFT.name());
        article.setLatestVersionNo(1);
        article.setViewCount(0L);
        article.setDeleted(0);
        articleMapper.insert(article);
        BlogArticleVersion version = buildVersion(article.getId(), 1, request);
        versionMapper.insert(version);
        saveTags(article.getId(), request.getTagIds());
        fileService.bindReferences(allFileIds(request), BlogConstants.AUDIT_BIZ_TYPE, article.getId(), 1);
        return article.getId();
    }

    @Override
    @Transactional
    public void update(Long id, BlogSaveDTO request, Long userId) {
        BlogArticle article = owned(id, userId);
        if (ArticleStatus.OFFLINE.name().equals(article.getStatus()))
            throw new ServiceException(409, "已下线文章不能编辑");
        validate(request, userId);
        int nextVersion = article.getLatestVersionNo() + 1;
        versionMapper.insert(buildVersion(id, nextVersion, request));
        int updated = articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, id)
                .eq(BlogArticle::getAuthorId, userId).eq(BlogArticle::getLatestVersionNo, article.getLatestVersionNo())
                .eq(BlogArticle::getDeleted, 0).set(BlogArticle::getLatestVersionNo, nextVersion)
                .set(BlogArticle::getStatus, ArticleStatus.DRAFT.name()).set(BlogArticle::getAuditReason, null)
                .set(BlogArticle::getCategoryId, request.getCategoryId()).set(BlogArticle::getOriginalType, request.getType())
                .set(BlogArticle::getSourceName, request.getSourceName()).set(BlogArticle::getSourceUrl, request.getSourceUrl()));
        if (updated == 0) throw new ServiceException(409, "文章已被并发修改，请刷新后重试");
        saveTags(id, request.getTagIds());
        fileService.bindReferences(allFileIds(request), BlogConstants.AUDIT_BIZ_TYPE, id, nextVersion);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        owned(id, userId);
        int updated = articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, id)
                .eq(BlogArticle::getAuthorId, userId).eq(BlogArticle::getDeleted, 0).set(BlogArticle::getDeleted, 1));
        if (updated == 0) throw new ServiceException(409, "文章状态已变化");
        eventPublisher.publishEvent(new SearchIndexRefreshEvent("BLOG", id));
    }

    @Override
    @Transactional
    public void submit(Long id, Long userId) {
        BlogArticle article = owned(id, userId);
        BlogArticleVersion version = version(id, article.getLatestVersionNo());
        if (!VersionStatus.DRAFT.name().equals(version.getStatus()) || !ArticleStatus.DRAFT.name().equals(article.getStatus())) {
            throw new ServiceException(409, "仅最新草稿可以提交审核");
        }
        int locked = versionMapper.update(null, new LambdaUpdateWrapper<BlogArticleVersion>().eq(BlogArticleVersion::getId, version.getId())
                .eq(BlogArticleVersion::getStatus, VersionStatus.DRAFT.name()).set(BlogArticleVersion::getStatus, VersionStatus.PENDING.name())
                .set(BlogArticleVersion::getAuditReason, null));
        if (locked == 0) throw new ServiceException(409, "版本状态已变化");
        int updated = articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, id)
                .eq(BlogArticle::getAuthorId, userId).eq(BlogArticle::getLatestVersionNo, version.getVersionNo())
                .eq(BlogArticle::getStatus, ArticleStatus.DRAFT.name()).set(BlogArticle::getStatus, ArticleStatus.PENDING.name())
                .set(BlogArticle::getAuditReason, null));
        if (updated == 0) throw new ServiceException(409, "文章状态已变化");
        Set<Long> fileIds = new LinkedHashSet<>();
        if (version.getCoverFileId() != null) fileIds.add(version.getCoverFileId());
        auditService.submit(BlogConstants.AUDIT_BIZ_TYPE, id, version.getVersionNo(), userId,
                version.getTitle() + "\n" + version.getSummary() + "\n" + version.getContent(), fileIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogVersionVO> versions(Long id, BlogQueryDTO query, Long userId) {
        owned(id, userId);
        Page<BlogArticleVersion> page = versionMapper.selectPage(new Page<>(pageNo(query), pageSize(query)),
                new LambdaQueryWrapper<BlogArticleVersion>().eq(BlogArticleVersion::getArticleId, id).orderByDesc(BlogArticleVersion::getVersionNo));
        return PageResult.of(page.getRecords().stream().map(item -> {
            BlogVersionVO vo = new BlogVersionVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogArticleVO> adminPage(BlogQueryDTO query) {
        Page<BlogArticle> page = articleMapper.selectAdminPage(new Page<>(pageNo(query), pageSize(query)), query.getStatus(), cleanKeyword(query.getKeyword()));
        return PageResult.of(page.getRecords().stream().map(article -> toVO(article, version(article.getId(), article.getLatestVersionNo()), false)).toList(),
                page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional(readOnly = true)
    public BlogArticleVO adminDetail(Long id) {
        BlogArticle article = articleMapper.selectById(id);
        if (article == null || article.getDeleted() == 1) throw new ServiceException(404, "文章不存在");
        return toVO(article, version(id, article.getLatestVersionNo()), false);
    }

    @Override
    @Transactional
    public void adminStatus(Long id, AdminBlogStatusDTO request) {
        BlogArticle article = articleMapper.selectById(id);
        if (article == null || article.getDeleted() == 1) throw new ServiceException(404, "文章不存在");
        if (ArticleStatus.PUBLISHED.name().equals(request.getStatus())) {
            if (article.getPublishedVersionNo() == null) throw new ServiceException(409, "尚无审核通过的公开版本");
            BlogArticleVersion published = version(id, article.getPublishedVersionNo());
            if (!VersionStatus.APPROVED.name().equals(published.getStatus()))
                throw new ServiceException(409, "公开版本未审核通过");
        }
        articleMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, id).eq(BlogArticle::getDeleted, 0)
                .set(BlogArticle::getStatus, request.getStatus()).set(BlogArticle::getAuditReason, request.getReason()));
        eventPublisher.publishEvent(new SearchIndexRefreshEvent("BLOG", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlogCategoryVO> categoryPage(BlogQueryDTO query) {
        Page<BlogCategory> page = categoryMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), new LambdaQueryWrapper<BlogCategory>()
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(), BlogCategory::getName, cleanKeyword(query.getKeyword()))
                .orderByAsc(BlogCategory::getSortOrder).orderByDesc(BlogCategory::getId));
        return PageResult.of(page.getRecords().stream().map(this::categoryVO).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public Long createCategory(BlogCategoryDTO request) {
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(request, category);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional
    public void updateCategory(Long id, BlogCategoryDTO request) {
        if (categoryMapper.selectById(id) == null) throw new ServiceException(404, "分类不存在");
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(request, category);
        category.setId(id);
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Long used = articleMapper.selectCount(new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getCategoryId, id).eq(BlogArticle::getDeleted, 0));
        if (used > 0) throw new ServiceException(409, "分类下仍有文章，不能删除");
        if (categoryMapper.deleteById(id) == 0) throw new ServiceException(404, "分类不存在");
    }

    private void validate(BlogSaveDTO request, Long userId) {
        if (DANGEROUS_HTML.matcher(request.getTitle()).find() || DANGEROUS_HTML.matcher(request.getContent()).find()
                || request.getSummary() != null && DANGEROUS_HTML.matcher(request.getSummary()).find()) {
            throw new ServiceException(400, "内容包含危险HTML或脚本");
        }
        BlogCategory category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() != 1) throw new ServiceException(400, "分类不存在或已停用");
        if ("REPRINT".equals(request.getType())) {
            if (request.getSourceName() == null || request.getSourceName().isBlank() || request.getSourceUrl() == null || request.getSourceUrl().isBlank()) {
                throw new ServiceException(400, "转载文章必须填写来源名称和来源地址");
            }
            try {
                URI uri = URI.create(request.getSourceUrl());
                if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                throw new ServiceException(400, "转载来源地址必须是有效的HTTP或HTTPS地址");
            }
        } else {
            request.setSourceName(null);
            request.setSourceUrl(null);
        }
        Set<Long> ids = allFileIds(request);
        fileService.requireOwnedImages(ids, userId);
    }

    private BlogArticleVersion buildVersion(Long articleId, int versionNo, BlogSaveDTO request) {
        BlogArticleVersion version = new BlogArticleVersion();
        BeanUtils.copyProperties(request, version);
        version.setArticleId(articleId);
        version.setVersionNo(versionNo);
        String summary = request.getSummary() == null || request.getSummary().isBlank()
                ? request.getContent().replaceAll("<[^>]*>", "").trim() : request.getSummary().trim();
        version.setSummary(summary.length() > 500 ? summary.substring(0, 500) : summary);
        version.setContentFileIds("[]");
        version.setStatus(VersionStatus.DRAFT.name());
        return version;
    }

    private Set<Long> allFileIds(BlogSaveDTO request) {
        Set<Long> ids = new LinkedHashSet<>();
        if (request.getCoverFileId() != null) ids.add(request.getCoverFileId());
        return ids;
    }

    private void saveTags(Long articleId, List<Long> tagIds) {
        articleTagMapper.delete(new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, articleId));
        if (tagIds == null || tagIds.isEmpty()) return;
        Set<Long> distinctIds = new LinkedHashSet<>(tagIds);
        if (distinctIds.contains(null) || tagMapper.selectByIds(distinctIds).size() != distinctIds.size()) {
            throw new ServiceException(400, "标签不存在");
        }
        for (Long tagId : distinctIds) {
            BlogArticleTag relation = new BlogArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            articleTagMapper.insert(relation);
        }
    }

    private List<String> tags(Long articleId) {
        List<BlogArticleTag> relations = articleTagMapper.selectList(new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, articleId));
        if (relations.isEmpty()) return List.of();
        List<Long> ids = relations.stream().map(BlogArticleTag::getTagId).toList();
        return tagMapper.selectByIds(ids).stream().map(BlogTag::getName).toList();
    }

    private BlogArticleVO toVO(BlogArticle article, BlogArticleVersion version, boolean publicView) {
        BlogArticleVO vo = publicView ? converter.toPublicVO(article, version, tags(article.getId())) : converter.toVO(article, version, tags(article.getId()));
        vo.setTagIds(articleTagMapper.selectList(new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, article.getId()))
                .stream().map(BlogArticleTag::getTagId).toList());
        return vo;
    }

    private BlogArticleVersion version(Long articleId, Integer versionNo) {
        BlogArticleVersion version = versionMapper.selectOne(new LambdaQueryWrapper<BlogArticleVersion>()
                .eq(BlogArticleVersion::getArticleId, articleId).eq(BlogArticleVersion::getVersionNo, versionNo));
        if (version == null) throw new ServiceException(500, "文章版本指针异常");
        return version;
    }

    private BlogArticle owned(Long id, Long userId) {
        BlogArticle article = articleMapper.selectById(id);
        if (article == null || article.getDeleted() == 1) throw new ServiceException(404, "文章不存在");
        if (!article.getAuthorId().equals(userId)) throw new ServiceException(403, "无权操作他人的文章");
        return article;
    }

    private BlogCategoryVO categoryVO(BlogCategory category) {
        BlogCategoryVO vo = new BlogCategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    private long pageNo(BlogQueryDTO query) {
        return Math.max(1, query.getPage());
    }

    private long pageSize(BlogQueryDTO query) {
        return Math.min(BlogConstants.MAX_PAGE_SIZE, Math.max(1, query.getSize()));
    }

    private String cleanKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}
