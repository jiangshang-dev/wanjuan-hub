package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.membership.entity.MembershipEntities.CommissionConfig;
import com.heima.modules.membership.entity.MembershipEntities.DailyUsage;
import com.heima.modules.membership.entity.MembershipEntities.ResourceClaim;
import com.heima.modules.membership.entity.MembershipEntities.UserMembership;
import com.heima.modules.membership.mapper.MembershipMappers.DailyUsageMapper;
import com.heima.modules.membership.mapper.MembershipMappers.ResourceClaimMapper;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.resource.service.ResourceClaimService;
import com.heima.modules.resource.vo.ResourceClaimVO;
import com.heima.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResourceClaimServiceImpl implements ResourceClaimService {
    private final ResourceInfoMapper resourceMapper;
    private final ResourceVersionMapper versionMapper;
    private final ResourceEntitlementMapper entitlementMapper;
    private final ResourceClaimMapper claimMapper;
    private final DailyUsageMapper dailyUsageMapper;
    private final MembershipService membershipService;
    private final WalletService walletService;
    private final Clock clock;

    @Override
    @Transactional
    public ResourceClaimVO claim(Long userId, Long resourceId) {
        ResourceInfo resource = publicResource(resourceId);
        ResourceVersion version = versionMapper.selectOne(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resourceId).eq(ResourceVersion::getVersionNo, resource.getPublishedVersionNo()));
        if (version == null || AccessType.valueOf(version.getAccessType()).normalize() != AccessType.MEMBER) {
            throw new ServiceException(409, "该资源不是会员资源");
        }
        ResourceEntitlement existing = entitlement(resourceId, userId);
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            return duplicated(resourceId, existing.getGrantedAt(), userId);
        }

        UserMembership membership = membershipService.lockActiveMembership(userId);
        LocalDate today = LocalDate.now(clock);
        dailyUsageMapper.createIgnore(userId, today);
        DailyUsage usage = dailyUsageMapper.selectForUpdate(userId, today);
        if (usage == null) throw new ServiceException(500, "会员每日额度初始化失败");
        ResourceClaim oldClaim = claimMapper.selectOne(new LambdaQueryWrapper<ResourceClaim>()
                .eq(ResourceClaim::getUserId, userId).eq(ResourceClaim::getResourceId, resourceId));
        if (oldClaim != null) return duplicated(resourceId, oldClaim.getCreateTime(), userId);
        if (dailyUsageMapper.consume(userId, today, membership.getDailyResourceLimit()) == 0) {
            throw new ServiceException(429, "今日会员资源领取额度已用完");
        }

        grantMembershipAccess(existing, userId, resourceId);
        ResourceClaim claim = new ResourceClaim();
        claim.setUserId(userId);
        claim.setResourceId(resourceId);
        claim.setClaimDate(today);
        claimMapper.insert(claim);

        BigDecimal commission = BigDecimal.ZERO;
        if (!resource.getAuthorId().equals(userId)) {
            CommissionConfig config = membershipService.commissionConfig();
            commission = config.getMemberFixedAmount();
            walletService.credit(resource.getAuthorId(), "MEMBER_RESOURCE_COMMISSION", String.valueOf(claim.getId()),
                    commission, "会员资源首次有效领取返佣，资源ID：" + resourceId);
        }
        int used = usage.getUsedCount() + 1;
        return result(resourceId, false, commission, claim.getCreateTime() == null ? LocalDateTime.now(clock) : claim.getCreateTime(),
                used, Math.max(0, membership.getDailyResourceLimit() - used));
    }

    private void grantMembershipAccess(ResourceEntitlement existing, Long userId, Long resourceId) {
        if (existing != null) {
            int updated = entitlementMapper.update(null, new LambdaUpdateWrapper<ResourceEntitlement>()
                    .eq(ResourceEntitlement::getId, existing.getId()).ne(ResourceEntitlement::getStatus, "ACTIVE")
                    .set(ResourceEntitlement::getSourceType, "MEMBER").set(ResourceEntitlement::getSourceOrderNo, null)
                    .set(ResourceEntitlement::getAmount, null).set(ResourceEntitlement::getPointsAmount, null)
                    .set(ResourceEntitlement::getStatus, "ACTIVE").set(ResourceEntitlement::getRevokeReason, null)
                    .set(ResourceEntitlement::getRevokedAt, null).set(ResourceEntitlement::getGrantedAt, LocalDateTime.now(clock)));
            if (updated == 0) throw new ServiceException(409, "资源权益已变化，请重试");
            return;
        }
        entitlementMapper.grantIgnore(userId, resourceId, "MEMBER", null, null, null);
        ResourceEntitlement granted = entitlement(resourceId, userId);
        if (granted == null || !"ACTIVE".equals(granted.getStatus()) || !"MEMBER".equals(granted.getSourceType())) {
            throw new ServiceException(409, "资源权益已变化，领取未生效");
        }
    }

    private ResourceClaimVO duplicated(Long resourceId, LocalDateTime claimedAt, Long userId) {
        var status = membershipService.status(userId);
        return result(resourceId, true, BigDecimal.ZERO, claimedAt, status.getUsedToday(), status.getRemainingToday());
    }

    private ResourceClaimVO result(Long resourceId, boolean duplicated, BigDecimal commission,
                                   LocalDateTime claimedAt, Integer used, Integer remaining) {
        ResourceClaimVO vo = new ResourceClaimVO();
        vo.setResourceId(resourceId);
        vo.setDuplicated(duplicated);
        vo.setCommissionAmount(commission);
        vo.setClaimedAt(claimedAt);
        vo.setUsedToday(used);
        vo.setRemainingToday(remaining);
        return vo;
    }

    private ResourceEntitlement entitlement(Long resourceId, Long userId) {
        return entitlementMapper.selectOne(new LambdaQueryWrapper<ResourceEntitlement>()
                .eq(ResourceEntitlement::getUserId, userId).eq(ResourceEntitlement::getResourceId, resourceId));
    }

    private ResourceInfo publicResource(Long resourceId) {
        ResourceInfo resource = resourceMapper.selectOne(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getId, resourceId)
                .eq(ResourceInfo::getDeleted, 0).eq(ResourceInfo::getOnline, 1).eq(ResourceInfo::getInvalid, 0)
                .isNotNull(ResourceInfo::getPublishedVersionNo));
        if (resource == null) throw new ServiceException(404, "资源不存在或未公开");
        return resource;
    }
}
