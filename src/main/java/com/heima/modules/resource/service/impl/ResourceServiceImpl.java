package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.file.service.FileService;
import com.heima.modules.resource.constant.ResourceConstants;
import com.heima.modules.resource.dto.AdminResourceStatusDTO;
import com.heima.modules.resource.dto.ResourceLinkDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceSaveDTO;
import com.heima.modules.resource.entity.ResourceCategory;
import com.heima.modules.resource.entity.ResourceDiskType;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceLink;
import com.heima.modules.resource.entity.ResourceTag;
import com.heima.modules.resource.entity.ResourceTagRelation;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.enums.ResourceStatus;
import com.heima.modules.resource.enums.VersionStatus;
import com.heima.modules.resource.mapper.ResourceCategoryMapper;
import com.heima.modules.resource.mapper.ResourceDiskTypeMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceLinkMapper;
import com.heima.modules.resource.mapper.ResourceTagMapper;
import com.heima.modules.resource.mapper.ResourceTagRelationMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.resource.service.ResourceAccessService;
import com.heima.modules.resource.service.ResourceCryptoService;
import com.heima.modules.resource.service.ResourceService;
import com.heima.modules.resource.service.ResourceUrlValidator;
import com.heima.modules.resource.vo.ResourceVO;
import com.heima.modules.resource.vo.ResourceVersionVO;
import com.heima.modules.search.event.SearchIndexRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern DANGEROUS_HTML = Pattern.compile(
            "<\\s*(script|iframe|object|embed|svg|math)\\b|on[a-z]+\\s*=|javascript\\s*:|srcdoc\\s*=|data\\s*:\\s*text/html",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final ResourceInfoMapper resourceMapper;
    private final ResourceVersionMapper versionMapper;
    private final ResourceCategoryMapper categoryMapper;
    private final ResourceTagMapper tagMapper;
    private final ResourceTagRelationMapper tagRelationMapper;
    private final ResourceLinkMapper linkMapper;
    private final ResourceDiskTypeMapper diskTypeMapper;
    private final ResourceAccessService accessService;
    private final ResourceCryptoService cryptoService;
    private final ResourceUrlValidator urlValidator;
    private final FileService fileService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceVO> publicPage(ResourceQueryDTO query) {
        Page<ResourceInfo> page = resourceMapper.selectPublicPage(new Page<>(pageNo(query), pageSize(query)), query.getCategoryId(),
                query.getTagId(), query.getAccessType(), cleanKeyword(query.getKeyword()));
        return PageResult.of(page.getRecords().stream().map(resource -> toVO(resource, version(resource.getId(), resource.getPublishedVersionNo()), true)).toList(),
                page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public ResourceVO publicDetail(Long id, Long userId, boolean admin) {
        ResourceInfo resource = publicResource(id);
        ResourceVersion version = version(id, resource.getPublishedVersionNo());
        ResourceVO vo = toVO(resource, version, true);
        vo.setAccessGranted(accessService.decide(resource, version, userId, admin).granted());
        resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, id).setSql("view_count = view_count + 1"));
        vo.setViewCount(resource.getViewCount() + 1);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceVO> userPage(ResourceQueryDTO query, Long userId) {
        Page<ResourceInfo> page = resourceMapper.selectPage(new Page<>(pageNo(query), pageSize(query)),
                new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getAuthorId, userId).eq(ResourceInfo::getDeleted, 0)
                        .eq(query.getStatus() != null && !query.getStatus().isBlank(), ResourceInfo::getStatus, query.getStatus())
                        .orderByDesc(ResourceInfo::getId));
        return PageResult.of(page.getRecords().stream().map(resource -> toVO(resource, version(resource.getId(), resource.getLatestVersionNo()), false)).toList(),
                page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceVO userDetail(Long id, Long userId) {
        ResourceInfo resource = owned(id, userId);
        return toVO(resource, version(id, resource.getLatestVersionNo()), false);
    }

    @Override
    @Transactional
    public Long create(ResourceSaveDTO request, Long userId) {
        validate(request, userId);
        ResourceInfo resource = new ResourceInfo();
        resource.setAuthorId(userId);
        resource.setCategoryId(request.getCategoryId());
        resource.setStatus(ResourceStatus.DRAFT.name());
        resource.setLatestVersionNo(1);
        resource.setOnline(0);
        resource.setInvalid(0);
        resource.setViewCount(0L);
        resource.setDeleted(0);
        resourceMapper.insert(resource);
        ResourceVersion version = buildVersion(resource.getId(), 1, request);
        versionMapper.insert(version);
        saveTags(resource.getId(), request.getTagIds());
        saveLinks(resource.getId(), 1, request.getLinks());
        fileService.bindReferences(allFileIds(request), ResourceConstants.AUDIT_BIZ_TYPE, resource.getId(), 1);
        return resource.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ResourceSaveDTO request, Long userId) {
        ResourceInfo resource = owned(id, userId);
        if (ResourceStatus.OFFLINE.name().equals(resource.getStatus()) || ResourceStatus.INVALID.name().equals(resource.getStatus())) {
            throw new ServiceException(409, "已下线或失效资源不能编辑");
        }
        validate(request, userId);
        int nextVersion = resource.getLatestVersionNo() + 1;
        versionMapper.insert(buildVersion(id, nextVersion, request));
        int updated = resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, id)
                .eq(ResourceInfo::getAuthorId, userId).eq(ResourceInfo::getLatestVersionNo, resource.getLatestVersionNo())
                .eq(ResourceInfo::getDeleted, 0).set(ResourceInfo::getLatestVersionNo, nextVersion)
                .set(ResourceInfo::getStatus, ResourceStatus.DRAFT.name()).set(ResourceInfo::getAuditReason, null)
                .set(ResourceInfo::getCategoryId, request.getCategoryId()));
        if (updated == 0) throw new ServiceException(409, "资源已被并发修改，请刷新后重试");
        saveTags(id, request.getTagIds());
        saveLinks(id, nextVersion, request.getLinks());
        fileService.bindReferences(allFileIds(request), ResourceConstants.AUDIT_BIZ_TYPE, id, nextVersion);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        owned(id, userId);
        int updated = resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, id)
                .eq(ResourceInfo::getAuthorId, userId).eq(ResourceInfo::getDeleted, 0)
                .set(ResourceInfo::getDeleted, 1).set(ResourceInfo::getOnline, 0));
        if (updated == 0) throw new ServiceException(409, "资源状态已变化");
        eventPublisher.publishEvent(new SearchIndexRefreshEvent("RESOURCE", id));
    }

    @Override
    @Transactional
    public void submit(Long id, Long userId) {
        ResourceInfo resource = owned(id, userId);
        ResourceVersion version = version(id, resource.getLatestVersionNo());
        if (!VersionStatus.DRAFT.name().equals(version.getStatus()) || !ResourceStatus.DRAFT.name().equals(resource.getStatus())) {
            throw new ServiceException(409, "仅最新草稿可以提交审核");
        }
        List<ResourceLink> links = linkMapper.selectList(new LambdaQueryWrapper<ResourceLink>().eq(ResourceLink::getResourceId, id)
                .eq(ResourceLink::getVersionNo, version.getVersionNo()).eq(ResourceLink::getStatus, "ENABLED"));
        if (links.isEmpty()) throw new ServiceException(400, "发布资源至少需要一个有效网盘链接");
        for (ResourceLink link : links) {
            ResourceDiskType disk = diskTypeMapper.selectById(link.getDiskTypeId());
            urlValidator.validate(cryptoService.decrypt(link.getUrlCiphertext()), disk);
        }
        int locked = versionMapper.update(null, new LambdaUpdateWrapper<ResourceVersion>().eq(ResourceVersion::getId, version.getId())
                .eq(ResourceVersion::getStatus, VersionStatus.DRAFT.name()).set(ResourceVersion::getStatus, VersionStatus.PENDING.name())
                .set(ResourceVersion::getAuditReason, null));
        if (locked == 0) throw new ServiceException(409, "版本状态已变化");
        int updated = resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, id)
                .eq(ResourceInfo::getAuthorId, userId).eq(ResourceInfo::getLatestVersionNo, version.getVersionNo())
                .eq(ResourceInfo::getStatus, ResourceStatus.DRAFT.name()).set(ResourceInfo::getStatus, ResourceStatus.PENDING.name())
                .set(ResourceInfo::getAuditReason, null));
        if (updated == 0) throw new ServiceException(409, "资源状态已变化");
        Set<Long> fileIds = new LinkedHashSet<>();
        if (version.getCoverFileId() != null) fileIds.add(version.getCoverFileId());
        fileIds.addAll(attachmentIds(version));
        auditService.submit(ResourceConstants.AUDIT_BIZ_TYPE, id, version.getVersionNo(), userId,
                version.getTitle() + "\n" + version.getSummary() + "\n" + version.getDescription() + "\n" + version.getCopyrightNotice(), fileIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceVersionVO> versions(Long id, ResourceQueryDTO query, Long userId) {
        owned(id, userId);
        Page<ResourceVersion> page = versionMapper.selectPage(new Page<>(pageNo(query), pageSize(query)),
                new LambdaQueryWrapper<ResourceVersion>().eq(ResourceVersion::getResourceId, id).orderByDesc(ResourceVersion::getVersionNo));
        return PageResult.of(page.getRecords().stream().map(item -> {
            ResourceVersionVO vo = new ResourceVersionVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceVO> adminPage(ResourceQueryDTO query) {
        Page<ResourceInfo> page = resourceMapper.selectAdminPage(new Page<>(pageNo(query), pageSize(query)), query.getCategoryId(),
                query.getStatus(), cleanKeyword(query.getKeyword()));
        return PageResult.of(page.getRecords().stream().map(resource -> toVO(resource, version(resource.getId(), resource.getLatestVersionNo()), false)).toList(),
                page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public void adminStatus(Long id, AdminResourceStatusDTO request) {
        ResourceInfo resource = resourceMapper.selectById(id);
        if (resource == null || Integer.valueOf(1).equals(resource.getDeleted())) throw new ServiceException(404, "资源不存在");
        LambdaUpdateWrapper<ResourceInfo> update = new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, id).eq(ResourceInfo::getDeleted, 0)
                .set(ResourceInfo::getStatus, request.getStatus()).set(ResourceInfo::getAuditReason, request.getReason());
        if ("PUBLISHED".equals(request.getStatus())) {
            if (resource.getPublishedVersionNo() == null) throw new ServiceException(409, "尚无审核通过的公开版本");
            ResourceVersion published = version(id, resource.getPublishedVersionNo());
            if (!VersionStatus.APPROVED.name().equals(published.getStatus())) throw new ServiceException(409, "公开版本未审核通过");
            update.set(ResourceInfo::getOnline, 1).set(ResourceInfo::getInvalid, 0);
        } else if ("INVALID".equals(request.getStatus())) {
            update.set(ResourceInfo::getOnline, 0).set(ResourceInfo::getInvalid, 1);
        } else {
            update.set(ResourceInfo::getOnline, 0);
        }
        resourceMapper.update(null, update);
        eventPublisher.publishEvent(new SearchIndexRefreshEvent("RESOURCE", id));
    }

    private void validate(ResourceSaveDTO request, Long userId) {
        if (DANGEROUS_HTML.matcher(request.getTitle()).find() || DANGEROUS_HTML.matcher(request.getSummary()).find()
                || DANGEROUS_HTML.matcher(request.getDescription()).find() || DANGEROUS_HTML.matcher(request.getCopyrightNotice()).find()) {
            throw new ServiceException(400, "内容包含危险HTML或脚本");
        }
        ResourceCategory category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() != 1) throw new ServiceException(400, "分类不存在或已停用");
        AccessType accessType = AccessType.valueOf(request.getAccessType());
        if (accessType == AccessType.POINTS) {
            if (request.getPointsPrice() == null || request.getPrice() != null) throw new ServiceException(400, "积分资源仅填写有效积分价格");
        } else if (accessType == AccessType.PAID) {
            if (request.getPrice() == null || request.getPrice().signum() <= 0 || request.getPointsPrice() != null) {
                throw new ServiceException(400, "付费资源仅填写有效现金价格");
            }
        } else if (request.getPrice() != null || request.getPointsPrice() != null) {
            throw new ServiceException(400, "非收费资源不能填写价格");
        }
        fileService.requireOwnedImages(allFileIds(request), userId);
    }

    private ResourceVersion buildVersion(Long resourceId, int versionNo, ResourceSaveDTO request) {
        ResourceVersion version = new ResourceVersion();
        BeanUtils.copyProperties(request, version, "attachmentFileIds", "links");
        version.setResourceId(resourceId);
        version.setVersionNo(versionNo);
        version.setAttachmentFileIds(JSON.writeValueAsString(request.getAttachmentFileIds() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(request.getAttachmentFileIds()))));
        version.setStatus(VersionStatus.DRAFT.name());
        return version;
    }

    private void saveLinks(Long resourceId, Integer versionNo, List<ResourceLinkDTO> requests) {
        if (requests == null || requests.isEmpty()) return;
        for (ResourceLinkDTO request : requests) {
            ResourceDiskType disk = diskTypeMapper.selectById(request.getDiskTypeId());
            String url = urlValidator.validate(request.getUrl(), disk);
            ResourceLink link = new ResourceLink();
            link.setResourceId(resourceId);
            link.setVersionNo(versionNo);
            link.setDiskTypeId(request.getDiskTypeId());
            link.setUrlCiphertext(cryptoService.encrypt(url));
            link.setExtractionCodeCiphertext(cryptoService.encrypt(request.getExtractionCode()));
            link.setLinkName(request.getLinkName().trim());
            link.setStatus("ENABLED");
            link.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
            linkMapper.insert(link);
        }
    }

    private void saveTags(Long resourceId, List<Long> tagIds) {
        tagRelationMapper.delete(new LambdaQueryWrapper<ResourceTagRelation>().eq(ResourceTagRelation::getResourceId, resourceId));
        if (tagIds == null || tagIds.isEmpty()) return;
        Set<Long> distinct = new LinkedHashSet<>(tagIds);
        if (distinct.contains(null) || tagMapper.selectByIds(distinct).size() != distinct.size()) throw new ServiceException(400, "标签不存在");
        for (Long tagId : distinct) {
            ResourceTagRelation relation = new ResourceTagRelation();
            relation.setResourceId(resourceId);
            relation.setTagId(tagId);
            tagRelationMapper.insert(relation);
        }
    }

    private Set<Long> allFileIds(ResourceSaveDTO request) {
        Set<Long> ids = new LinkedHashSet<>();
        if (request.getCoverFileId() != null) ids.add(request.getCoverFileId());
        if (request.getAttachmentFileIds() != null) ids.addAll(request.getAttachmentFileIds());
        return ids;
    }

    private ResourceVO toVO(ResourceInfo resource, ResourceVersion version, boolean publicView) {
        ResourceVO vo = new ResourceVO();
        BeanUtils.copyProperties(resource, vo);
        BeanUtils.copyProperties(version, vo);
        vo.setId(resource.getId());
        vo.setStatus(resource.getStatus());
        vo.setCreateTime(resource.getCreateTime());
        vo.setUpdateTime(resource.getUpdateTime());
        vo.setAttachmentFileIds(attachmentIds(version));
        List<ResourceTagRelation> relations = tagRelationMapper.selectList(new LambdaQueryWrapper<ResourceTagRelation>()
                .eq(ResourceTagRelation::getResourceId, resource.getId()).orderByAsc(ResourceTagRelation::getId));
        vo.setTagIds(relations.stream().map(ResourceTagRelation::getTagId).toList());
        vo.setTags(relations.isEmpty() ? List.of() : tagMapper.selectByIds(vo.getTagIds()).stream().map(ResourceTag::getName).toList());
        if (publicView) {
            vo.setStatus(null);
            vo.setVersionStatus(null);
            vo.setAuditReason(null);
        } else {
            vo.setVersionStatus(version.getStatus());
        }
        return vo;
    }

    private List<Long> attachmentIds(ResourceVersion version) {
        try {
            return JSON.readValue(version.getAttachmentFileIds(), new TypeReference<List<Long>>() {
            });
        } catch (RuntimeException e) {
            throw new ServiceException(500, "资源附件数据异常");
        }
    }

    private ResourceVersion version(Long resourceId, Integer versionNo) {
        ResourceVersion version = versionMapper.selectOne(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resourceId).eq(ResourceVersion::getVersionNo, versionNo));
        if (version == null) throw new ServiceException(500, "资源版本指针异常");
        return version;
    }

    private ResourceInfo publicResource(Long id) {
        ResourceInfo resource = resourceMapper.selectOne(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getId, id)
                .eq(ResourceInfo::getDeleted, 0).eq(ResourceInfo::getOnline, 1).eq(ResourceInfo::getInvalid, 0)
                .isNotNull(ResourceInfo::getPublishedVersionNo));
        if (resource == null) throw new ServiceException(404, "资源不存在或未公开");
        return resource;
    }

    private ResourceInfo owned(Long id, Long userId) {
        ResourceInfo resource = resourceMapper.selectById(id);
        if (resource == null || Integer.valueOf(1).equals(resource.getDeleted())) throw new ServiceException(404, "资源不存在");
        if (!resource.getAuthorId().equals(userId)) throw new ServiceException(403, "无权操作他人的资源");
        return resource;
    }

    private long pageNo(ResourceQueryDTO query) {
        return Math.max(1, query.getPage());
    }

    private long pageSize(ResourceQueryDTO query) {
        return Math.min(ResourceConstants.MAX_PAGE_SIZE, Math.max(1, query.getSize()));
    }

    private String cleanKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
