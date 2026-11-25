package com.heima.modules.search.service;

import com.heima.common.PageResult;
import com.heima.modules.search.dto.SearchDtos.SearchResultVO;

import java.util.List;

public interface SearchService {
    PageResult<SearchResultVO> search(String keyword, String type, long page, long size, Long userId);
    List<String> hotKeywords(int limit);
    List<String> history(Long userId, int limit);
}
