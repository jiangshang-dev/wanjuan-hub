package com.heima.modules.resource.service;

import com.heima.common.PageResult;
import com.heima.modules.resource.dto.ResourceInvalidReportDTO;
import com.heima.modules.resource.dto.ResourceInvalidReportHandleDTO;
import com.heima.modules.resource.dto.ResourceReportQueryDTO;
import com.heima.modules.resource.vo.ResourceInvalidReportVO;

public interface ResourceReportService {
    Long create(Long resourceId, ResourceInvalidReportDTO request, Long reporterId);
    PageResult<ResourceInvalidReportVO> page(ResourceReportQueryDTO query);
    void handle(Long id, ResourceInvalidReportHandleDTO request, Long handlerId);
}
