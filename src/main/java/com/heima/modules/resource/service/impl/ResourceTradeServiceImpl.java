package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.points.service.PointsService;
import com.heima.modules.resource.service.ResourceTradeService;
import com.heima.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResourceTradeServiceImpl implements ResourceTradeService {
    private final ResourceInfoMapper resourceMapper;
    private final ResourceVersionMapper versionMapper;
    private final ResourceEntitlementMapper entitlementMapper;
    private final PointsService pointsService;
    private final MembershipService membershipService;
    private final WalletService walletService;

    @Override
    @Transactional(readOnly = true)
    public PurchaseItem getPaidPurchase(Long resourceId, Long buyerId) {
        ResourceInfo resource = publicResource(resourceId);
        ResourceVersion version = version(resourceId, resource.getPublishedVersionNo());
        if (!AccessType.PAID.name().equals(version.getAccessType())) throw new ServiceException(409, "该资源不是付费资源");
        if (resource.getAuthorId().equals(buyerId) || hasActive(buyerId, resourceId)) {
            throw new ServiceException(409, "已拥有该资源，请勿重复购买");
        }
        if (version.getPrice() == null || version.getPrice().signum() <= 0) throw new ServiceException(409, "资源价格状态异常");
        return new PurchaseItem(resourceId, version.getTitle(), version.getPrice(), resource.getAuthorId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void grantPaidAccess(Long userId, Long resourceId, String orderNo) {
        if (userId == null || resourceId == null || orderNo == null || orderNo.isBlank()) {
            throw new ServiceException(400, "授权用户、资源和订单号不能为空");
        }
        ResourceInfo resource = resourceMapper.selectById(resourceId);
        if (resource == null || Integer.valueOf(1).equals(resource.getDeleted())) throw new ServiceException(404, "资源不存在");
        ResourceVersion version = version(resourceId, resource.getPublishedVersionNo() == null ? resource.getLatestVersionNo() : resource.getPublishedVersionNo());
        ResourceEntitlement existing = entitlement(userId, resourceId);
        if (existing != null && "PAID".equals(existing.getSourceType()) && orderNo.equals(existing.getSourceOrderNo())) return;
        if (existing != null && "ACTIVE".equals(existing.getStatus())) throw new ServiceException(409, "用户已拥有该资源访问权益");
        if (existing != null) {
            int updated = entitlementMapper.update(null, new LambdaUpdateWrapper<ResourceEntitlement>()
                    .eq(ResourceEntitlement::getId, existing.getId()).eq(ResourceEntitlement::getStatus, "REVOKED")
                    .set(ResourceEntitlement::getSourceType, "PAID").set(ResourceEntitlement::getSourceOrderNo, orderNo)
                    .set(ResourceEntitlement::getAmount, version.getPrice()).set(ResourceEntitlement::getPointsAmount, null)
                    .set(ResourceEntitlement::getStatus, "ACTIVE").set(ResourceEntitlement::getRevokeReason, null)
                    .set(ResourceEntitlement::getRevokedAt, null).set(ResourceEntitlement::getGrantedAt, LocalDateTime.now()));
            if (updated == 0) throw new ServiceException(409, "资源权益已变化，请重试");
            return;
        }
        entitlementMapper.grantIgnore(userId, resourceId, "PAID", orderNo, version.getPrice(), null);
        ResourceEntitlement granted = entitlement(userId, resourceId);
        if (granted == null || !"PAID".equals(granted.getSourceType()) || !orderNo.equals(granted.getSourceOrderNo())) {
            throw new ServiceException(409, "订单授权冲突，请核对订单与资源");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void revokePaidAccess(Long userId, Long resourceId, String reason) {
        if (userId == null || resourceId == null) throw new ServiceException(400, "用户和资源不能为空");
        entitlementMapper.revokePaid(userId, resourceId, reason == null || reason.isBlank() ? "订单退款或撤销" : reason);
    }

    @Override
    @Transactional
    public void redeemPoints(Long userId, Long resourceId) {
        ResourceInfo resource = publicResource(resourceId);
        ResourceVersion version = version(resourceId, resource.getPublishedVersionNo());
        if (!AccessType.POINTS.name().equals(version.getAccessType())) throw new ServiceException(409, "该资源不支持积分兑换");
        if (resource.getAuthorId().equals(userId) || hasActive(userId, resourceId)) throw new ServiceException(409, "已拥有该资源，请勿重复兑换");
        Integer points = version.getPointsPrice();
        if (points == null || points <= 0) throw new ServiceException(409, "资源积分价格状态异常");
        if (hasActive(userId, resourceId)) throw new ServiceException(409, "已拥有该资源，请勿重复兑换");
        pointsService.redeem(userId, resourceId, points);
        entitlementMapper.grantIgnore(userId, resourceId, "POINTS", null, null, points);
        ResourceEntitlement granted = entitlement(userId, resourceId);
        if (granted == null || !"ACTIVE".equals(granted.getStatus()) || !"POINTS".equals(granted.getSourceType())) {
            throw new ServiceException(409, "资源权益已变化，兑换未生效");
        }
        if (!resource.getAuthorId().equals(userId)) {
            BigDecimal commission = membershipService.commissionConfig().getPointsUnitPrice().multiply(BigDecimal.valueOf(points));
            walletService.credit(resource.getAuthorId(), "POINTS_RESOURCE_COMMISSION", userId + ":" + resourceId,
                    commission, "积分资源兑换返佣，扣除积分：" + points + "，资源ID：" + resourceId);
        }
    }

    private ResourceInfo publicResource(Long resourceId) {
        ResourceInfo resource = resourceMapper.selectOne(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getId, resourceId)
                .eq(ResourceInfo::getDeleted, 0).eq(ResourceInfo::getOnline, 1).eq(ResourceInfo::getInvalid, 0)
                .isNotNull(ResourceInfo::getPublishedVersionNo));
        if (resource == null) throw new ServiceException(404, "资源不存在或未公开");
        return resource;
    }

    private ResourceVersion version(Long resourceId, Integer versionNo) {
        ResourceVersion version = versionMapper.selectOne(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resourceId).eq(ResourceVersion::getVersionNo, versionNo));
        if (version == null) throw new ServiceException(500, "资源版本指针异常");
        return version;
    }

    private boolean hasActive(Long userId, Long resourceId) {
        if (userId == null) return false;
        return entitlementMapper.selectCount(new LambdaQueryWrapper<ResourceEntitlement>().eq(ResourceEntitlement::getUserId, userId)
                .eq(ResourceEntitlement::getResourceId, resourceId).eq(ResourceEntitlement::getStatus, "ACTIVE")) > 0;
    }

    private ResourceEntitlement entitlement(Long userId, Long resourceId) {
        return entitlementMapper.selectOne(new LambdaQueryWrapper<ResourceEntitlement>().eq(ResourceEntitlement::getUserId, userId)
                .eq(ResourceEntitlement::getResourceId, resourceId));
    }
}
