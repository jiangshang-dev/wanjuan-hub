package com.heima.modules.search.service;

import com.heima.modules.search.dto.SearchDtos.SearchCompensationVO;
import com.heima.modules.search.dto.SearchDtos.SearchRebuildVO;

public interface SearchMaintenanceService {
    SearchCompensationVO compensate(String contentType, Long contentId);

    SearchRebuildVO rebuildAll();
}
