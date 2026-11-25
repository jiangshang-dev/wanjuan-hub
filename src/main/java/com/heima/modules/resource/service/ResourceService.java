package com.heima.modules.resource.service;

import com.heima.common.PageResult;
import com.heima.modules.resource.dto.AdminResourceStatusDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceSaveDTO;
import com.heima.modules.resource.vo.ResourceVO;
import com.heima.modules.resource.vo.ResourceVersionVO;

public interface ResourceService {
    PageResult<ResourceVO> publicPage(ResourceQueryDTO query);
    ResourceVO publicDetail(Long id, Long userId, boolean admin);
    PageResult<ResourceVO> userPage(ResourceQueryDTO query, Long userId);
    ResourceVO userDetail(Long id, Long userId);
    Long create(ResourceSaveDTO request, Long userId);
    void update(Long id, ResourceSaveDTO request, Long userId);
    void delete(Long id, Long userId);
    void submit(Long id, Long userId);
    PageResult<ResourceVersionVO> versions(Long id, ResourceQueryDTO query, Long userId);
    PageResult<ResourceVO> adminPage(ResourceQueryDTO query);
    void adminStatus(Long id, AdminResourceStatusDTO request);
}
