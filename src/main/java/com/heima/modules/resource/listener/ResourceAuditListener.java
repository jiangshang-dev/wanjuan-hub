package com.heima.modules.resource.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.resource.constant.ResourceConstants;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.ResourceStatus;
import com.heima.modules.resource.enums.VersionStatus;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ResourceAuditListener {
    private final ResourceInfoMapper resourceMapper;
    private final ResourceVersionMapper versionMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompleted(AuditCompletedEvent event) {
        if (!ResourceConstants.AUDIT_BIZ_TYPE.equals(event.bizType())) return;
        ResourceInfo resource = resourceMapper.selectById(event.bizId());
        if (resource == null || Integer.valueOf(1).equals(resource.getDeleted())) return;
        ResourceVersion version = versionMapper.selectOne(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, event.bizId()).eq(ResourceVersion::getVersionNo, event.versionNo()));
        if (version == null || !resource.getLatestVersionNo().equals(event.versionNo())) {
            versionMapper.update(null, new LambdaUpdateWrapper<ResourceVersion>().eq(ResourceVersion::getResourceId, event.bizId())
                    .eq(ResourceVersion::getVersionNo, event.versionNo())
                    .in(ResourceVersion::getStatus, VersionStatus.PENDING.name(), VersionStatus.MANUAL_REVIEW.name(), VersionStatus.REJECTED.name())
                    .set(ResourceVersion::getStatus, VersionStatus.SUPERSEDED.name()).set(ResourceVersion::getAuditReason, "已有更新版本，本结果不再生效"));
            return;
        }
        if (event.status() == AuditStatus.APPROVED) {
            int versionUpdated = versionMapper.update(null, new LambdaUpdateWrapper<ResourceVersion>().eq(ResourceVersion::getId, version.getId())
                    .in(ResourceVersion::getStatus, VersionStatus.PENDING.name(), VersionStatus.MANUAL_REVIEW.name(), VersionStatus.REJECTED.name())
                    .set(ResourceVersion::getStatus, VersionStatus.APPROVED.name()).set(ResourceVersion::getAuditReason, null));
            if (versionUpdated == 0) return;
            int published = resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, resource.getId())
                    .eq(ResourceInfo::getLatestVersionNo, event.versionNo())
                    .in(ResourceInfo::getStatus, ResourceStatus.PENDING.name(), ResourceStatus.MANUAL_REVIEW.name(), ResourceStatus.REJECTED.name())
                    .eq(ResourceInfo::getDeleted, 0).set(ResourceInfo::getPublishedVersionNo, event.versionNo())
                    .set(ResourceInfo::getStatus, ResourceStatus.PUBLISHED.name()).set(ResourceInfo::getOnline, 1)
                    .set(ResourceInfo::getInvalid, 0).set(ResourceInfo::getAuditReason, null));
            if (published == 0) {
                versionMapper.update(null, new LambdaUpdateWrapper<ResourceVersion>().eq(ResourceVersion::getId, version.getId())
                        .set(ResourceVersion::getStatus, VersionStatus.SUPERSEDED.name()).set(ResourceVersion::getAuditReason, "资源状态已变化，未切换公开版本"));
            }
            return;
        }
        VersionStatus versionStatus = event.status() == AuditStatus.REJECTED ? VersionStatus.REJECTED : VersionStatus.MANUAL_REVIEW;
        ResourceStatus resourceStatus = event.status() == AuditStatus.REJECTED ? ResourceStatus.REJECTED : ResourceStatus.MANUAL_REVIEW;
        versionMapper.update(null, new LambdaUpdateWrapper<ResourceVersion>().eq(ResourceVersion::getId, version.getId())
                .in(ResourceVersion::getStatus, VersionStatus.PENDING.name(), VersionStatus.MANUAL_REVIEW.name(), VersionStatus.REJECTED.name())
                .set(ResourceVersion::getStatus, versionStatus.name()).set(ResourceVersion::getAuditReason, event.reason()));
        resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, resource.getId())
                .eq(ResourceInfo::getLatestVersionNo, event.versionNo())
                .in(ResourceInfo::getStatus, ResourceStatus.PENDING.name(), ResourceStatus.MANUAL_REVIEW.name(), ResourceStatus.REJECTED.name())
                .set(ResourceInfo::getStatus, resourceStatus.name()).set(ResourceInfo::getAuditReason, event.reason()));
    }
}
