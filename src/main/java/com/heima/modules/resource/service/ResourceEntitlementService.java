package com.heima.modules.resource.service;

import com.heima.common.PageResult;
import com.heima.modules.resource.dto.ResourceEntitlementQueryDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.vo.ResourceEntitlementAdminVO;
import com.heima.modules.resource.vo.ResourceEntitlementVO;

public interface ResourceEntitlementService {
    boolean hasActiveAccess(Long userId, Long resourceId);
    PageResult<ResourceEntitlementVO> userPage(Long userId, ResourceQueryDTO query);

    PageResult<ResourceEntitlementAdminVO> adminPage(ResourceEntitlementQueryDTO query);

    ResourceEntitlementAdminVO adminDetail(Long id);
}
