package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.service.FileService;
import com.heima.modules.resource.entity.ResourceAccessRecord;
import com.heima.modules.resource.entity.ResourceDiskType;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceLink;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.mapper.ResourceAccessRecordMapper;
import com.heima.modules.resource.mapper.ResourceDiskTypeMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceLinkMapper;
import com.heima.modules.resource.mapper.ResourceReplyMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.resource.service.ResourceAccessService;
import com.heima.modules.resource.service.ResourceCryptoService;
import com.heima.modules.resource.service.ResourceEntitlementService;
import com.heima.modules.resource.service.ResourceUrlValidator;
import com.heima.modules.resource.vo.ResourceAttachmentVO;
import com.heima.modules.resource.vo.ResourceLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceAccessServiceImpl implements ResourceAccessService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration ATTACHMENT_EXPIRE = Duration.ofMinutes(10);
    private final ResourceInfoMapper resourceMapper;
    private final ResourceVersionMapper versionMapper;
    private final ResourceLinkMapper linkMapper;
    private final ResourceDiskTypeMapper diskTypeMapper;
    private final ResourceAccessRecordMapper accessRecordMapper;
    private final ResourceReplyMapper replyMapper;
    private final ResourceEntitlementService entitlementService;
    private final ResourceCryptoService cryptoService;
    private final ResourceUrlValidator urlValidator;
    private final AuditService auditService;
    private final FileService fileService;

    @Override
    @Transactional(readOnly = true)
    public AccessDecision decide(ResourceInfo resource, ResourceVersion version, Long userId, boolean admin) {
        if (resource == null || version == null) return new AccessDecision(false, "资源不可用");
        if (admin || resource.getAuthorId().equals(userId)) return new AccessDecision(true, "作者或管理员");
        AccessType accessType;
        try {
            accessType = AccessType.valueOf(version.getAccessType());
        } catch (IllegalArgumentException | NullPointerException e) {
            return new AccessDecision(false, "资源访问类型异常");
        }
        return switch (accessType) {
            case FREE -> new AccessDecision(true, "免费资源");
            case GUEST_FREE -> new AccessDecision(true, "游客免费");
            case LOGIN_FREE -> userId == null ? new AccessDecision(false, "请先登录") : new AccessDecision(true, "登录免费");
            case REPLY_REQUIRED -> userId == null ? new AccessDecision(false, "请先登录并回复")
                    : hasApprovedReply(resource.getId(), userId) ? new AccessDecision(true, "已发布审核通过的回复")
                    : new AccessDecision(false, "请在关联内容发布审核通过的回复");
            case POINTS -> userId == null ? new AccessDecision(false, "请先登录并兑换")
                    : entitlementService.hasActiveAccess(userId, resource.getId()) ? new AccessDecision(true, "已兑换")
                    : new AccessDecision(false, "请使用积分兑换");
            case MEMBER -> userId == null ? new AccessDecision(false, "请先登录并领取")
                    : entitlementService.hasActiveAccess(userId, resource.getId()) ? new AccessDecision(true, "已领取")
                    : new AccessDecision(false, "请在会员有效期内领取后访问");
            case PAID -> userId == null ? new AccessDecision(false, "请先登录并购买")
                    : entitlementService.hasActiveAccess(userId, resource.getId()) ? new AccessDecision(true, "已购买")
                    : new AccessDecision(false, "请购买后访问");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAccess(Long resourceId, Long userId, boolean admin) {
        ResourceInfo resource = publicResource(resourceId);
        ResourceVersion version = publicVersion(resource);
        AccessDecision decision = decide(resource, version, userId, admin);
        if (decision.granted()) return;
        if (userId == null && !admin) throw new ServiceException(401, "请先登录后访问该资源");
        throw new ServiceException(403, decision.reason());
    }

    @Override
    @Transactional
    public List<ResourceLinkVO> links(Long resourceId, Long userId, boolean admin) {
        requireAccess(resourceId, userId, admin);
        ResourceInfo resource = publicResource(resourceId);
        List<ResourceLink> links = linkMapper.selectList(new LambdaQueryWrapper<ResourceLink>().eq(ResourceLink::getResourceId, resourceId)
                .eq(ResourceLink::getVersionNo, resource.getPublishedVersionNo()).eq(ResourceLink::getStatus, "ENABLED")
                .orderByAsc(ResourceLink::getSortOrder).orderByAsc(ResourceLink::getId));
        if (links.isEmpty()) return List.of();
        Map<Long, ResourceDiskType> disks = diskTypeMapper.selectByIds(links.stream().map(ResourceLink::getDiskTypeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ResourceDiskType::getId, Function.identity()));
        List<ResourceLinkVO> result = links.stream().map(link -> {
            ResourceDiskType disk = disks.get(link.getDiskTypeId());
            if (disk == null) throw new ServiceException(500, "网盘类型数据异常");
            String url = urlValidator.validate(cryptoService.decrypt(link.getUrlCiphertext()), disk);
            ResourceLinkVO vo = new ResourceLinkVO();
            vo.setId(link.getId());
            vo.setDiskTypeId(disk.getId());
            vo.setDiskCode(disk.getCode());
            vo.setDiskName(disk.getName());
            vo.setLinkName(link.getLinkName());
            vo.setUrl(url);
            vo.setExtractionCode(cryptoService.decrypt(link.getExtractionCodeCiphertext()));
            return vo;
        }).toList();
        for (ResourceLink link : links) record(userId, resourceId, "LINK", link.getId());
        return result;
    }

    @Override
    @Transactional
    public ResourceAttachmentVO attachment(Long resourceId, Long fileId, Long userId, boolean admin) {
        requireAccess(resourceId, userId, admin);
        ResourceInfo resource = publicResource(resourceId);
        ResourceVersion version = publicVersion(resource);
        if (!attachmentIds(version).contains(fileId)) throw new ServiceException(404, "附件不属于该资源公开版本");
        FileObject file = fileService.getRequired(fileId);
        ResourceAttachmentVO vo = new ResourceAttachmentVO();
        vo.setId(fileId);
        vo.setUrl(fileService.presign(file, ATTACHMENT_EXPIRE));
        vo.setExpiresAt(LocalDateTime.now().plus(ATTACHMENT_EXPIRE));
        record(userId, resourceId, "ATTACHMENT", fileId);
        return vo;
    }

    private boolean hasApprovedReply(Long resourceId, Long userId) {
        for (Long postId : replyMapper.selectPublishedPostIds(resourceId, userId)) {
            if (auditService.isApproved("FORUM_POST", postId)) return true;
        }
        for (Long commentId : replyMapper.selectPublishedCommentIds(resourceId, userId)) {
            if (auditService.isApproved("COMMENT", commentId)) return true;
        }
        return false;
    }

    private ResourceInfo publicResource(Long resourceId) {
        ResourceInfo resource = resourceMapper.selectOne(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getId, resourceId)
                .eq(ResourceInfo::getDeleted, 0).eq(ResourceInfo::getOnline, 1).eq(ResourceInfo::getInvalid, 0)
                .isNotNull(ResourceInfo::getPublishedVersionNo));
        if (resource == null) throw new ServiceException(404, "资源不存在或未公开");
        return resource;
    }

    private ResourceVersion publicVersion(ResourceInfo resource) {
        ResourceVersion version = versionMapper.selectOne(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resource.getId()).eq(ResourceVersion::getVersionNo, resource.getPublishedVersionNo()));
        if (version == null) throw new ServiceException(500, "资源公开版本指针异常");
        return version;
    }

    private List<Long> attachmentIds(ResourceVersion version) {
        try {
            return JSON.readValue(version.getAttachmentFileIds(), new TypeReference<List<Long>>() {
            });
        } catch (RuntimeException e) {
            throw new ServiceException(500, "资源附件数据异常");
        }
    }

    private void record(Long userId, Long resourceId, String targetType, Long targetId) {
        ResourceAccessRecord record = new ResourceAccessRecord();
        record.setUserId(userId);
        record.setResourceId(resourceId);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        accessRecordMapper.insert(record);
    }
}
