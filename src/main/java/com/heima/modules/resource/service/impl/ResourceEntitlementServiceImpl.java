package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.service.ResourceEntitlementService;
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
}
