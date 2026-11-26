package com.heima.modules.resource.service;

import com.heima.modules.resource.vo.ResourceClaimVO;

public interface ResourceClaimService {
    ResourceClaimVO claim(Long userId, Long resourceId);
}
