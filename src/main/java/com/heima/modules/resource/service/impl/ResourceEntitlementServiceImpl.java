package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.resource.dto.ResourceEntitlementQueryDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.service.ResourceEntitlementService;
import com.heima.modules.resource.vo.ResourceEntitlementAdminVO;
import com.heima.modules.resource.vo.ResourceEntitlementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceEntitlementServiceImpl implements ResourceEntitlementService {
    private final ResourceEntitlementMapper entitlementMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAccess(Long userId, Long resourceId) {
        if (userId == null) return false;
        return entitlementMapper.selectCount(new LambdaQueryWrapper<ResourceEntitlement>()
                .eq(ResourceEntitlement::getUserId, userId).eq(ResourceEntitlement::getResourceId, resourceId)
                .eq(ResourceEntitlement::getStatus, "ACTIVE")) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceEntitlementVO> userPage(Long userId, ResourceQueryDTO query) {
        long pageNo = Math.max(1, query.getPage());
        long pageSize = Math.min(100, Math.max(1, query.getSize()));
        Page<ResourceEntitlement> page = entitlementMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<ResourceEntitlement>().eq(ResourceEntitlement::getUserId, userId).orderByDesc(ResourceEntitlement::getId));
        return PageResult.of(page.getRecords().stream().map(item -> {
            ResourceEntitlementVO vo = new ResourceEntitlementVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), page.getTotal(), pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceEntitlementAdminVO> adminPage(ResourceEntitlementQueryDTO query) {
        LambdaQueryWrapper<ResourceEntitlement> wrapper = new LambdaQueryWrapper<ResourceEntitlement>()
                .eq(query.getUserId() != null, ResourceEntitlement::getUserId, query.getUserId())
                .eq(query.getResourceId() != null, ResourceEntitlement::getResourceId, query.getResourceId())
                .eq(query.getSourceOrderNo() != null && !query.getSourceOrderNo().isBlank(),
                        ResourceEntitlement::getSourceOrderNo, query.getSourceOrderNo())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), ResourceEntitlement::getStatus, query.getStatus())
                .orderByDesc(ResourceEntitlement::getId);
        Page<ResourceEntitlement> page = entitlementMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(ResourceEntitlementServiceImpl::adminVO).toList(),
                page.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceEntitlementAdminVO adminDetail(Long id) {
        ResourceEntitlement entitlement = entitlementMapper.selectById(id);
        if (entitlement == null) {
            throw new ServiceException(404, "资源授权记录不存在");
        }
        return adminVO(entitlement);
    }

    private static ResourceEntitlementAdminVO adminVO(ResourceEntitlement entitlement) {
        ResourceEntitlementAdminVO vo = new ResourceEntitlementAdminVO();
        BeanUtils.copyProperties(entitlement, vo);
        return vo;
    }
}
