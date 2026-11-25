package com.heima.modules.resource.service;

import com.heima.common.PageResult;
import com.heima.modules.resource.dto.ResourceCategoryDTO;
import com.heima.modules.resource.dto.ResourceDiskTypeDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceTagDTO;
import com.heima.modules.resource.vo.ResourceCategoryVO;
import com.heima.modules.resource.vo.ResourceDiskTypeVO;
import com.heima.modules.resource.vo.ResourceTagVO;

import java.util.List;

public interface ResourceCatalogService {
    List<ResourceCategoryVO> publicCategories();
    List<ResourceDiskTypeVO> publicDiskTypes();
    PageResult<ResourceCategoryVO> categoryPage(ResourceQueryDTO query);
    Long createCategory(ResourceCategoryDTO request);
    void updateCategory(Long id, ResourceCategoryDTO request);
    void deleteCategory(Long id);
    PageResult<ResourceTagVO> tagPage(ResourceQueryDTO query);
    Long createTag(ResourceTagDTO request);
    void updateTag(Long id, ResourceTagDTO request);
    void deleteTag(Long id);
    PageResult<ResourceDiskTypeVO> diskTypePage(ResourceQueryDTO query);
    Long createDiskType(ResourceDiskTypeDTO request);
    void updateDiskType(Long id, ResourceDiskTypeDTO request);
    void deleteDiskType(Long id);
}
