package com.heima.modules.resource.service;

import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.vo.ResourceAttachmentVO;
import com.heima.modules.resource.vo.ResourceLinkVO;

import java.util.List;

public interface ResourceAccessService {
    record AccessDecision(boolean granted, String reason) {
    }

    AccessDecision decide(ResourceInfo resource, ResourceVersion version, Long userId, boolean admin);
    void requireAccess(Long resourceId, Long userId, boolean admin);
    List<ResourceLinkVO> links(Long resourceId, Long userId, boolean admin);
    ResourceAttachmentVO attachment(Long resourceId, Long fileId, Long userId, boolean admin);
}
