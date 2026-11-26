package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.resource.dto.ResourceCategoryDTO;
import com.heima.modules.resource.dto.ResourceDiskTypeDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceTagDTO;
import com.heima.modules.resource.entity.ResourceCategory;
import com.heima.modules.resource.entity.ResourceDiskType;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceLink;
import com.heima.modules.resource.entity.ResourceTag;
import com.heima.modules.resource.entity.ResourceTagRelation;
import com.heima.modules.resource.mapper.ResourceCategoryMapper;
import com.heima.modules.resource.mapper.ResourceDiskTypeMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceLinkMapper;
import com.heima.modules.resource.mapper.ResourceTagMapper;
import com.heima.modules.resource.mapper.ResourceTagRelationMapper;
import com.heima.modules.resource.service.ResourceCatalogService;
import com.heima.modules.resource.service.ResourceUrlValidator;
import com.heima.modules.resource.vo.ResourceCategoryVO;
import com.heima.modules.resource.vo.ResourceDiskTypeVO;
import com.heima.modules.resource.vo.ResourceTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceCatalogServiceImpl implements ResourceCatalogService {
    private final ResourceCategoryMapper categoryMapper;
    private final ResourceTagMapper tagMapper;
    private final ResourceTagRelationMapper tagRelationMapper;
    private final ResourceDiskTypeMapper diskTypeMapper;
    private final ResourceInfoMapper resourceMapper;
    private final ResourceLinkMapper linkMapper;
    private final ResourceUrlValidator urlValidator;

    @Override
    @Transactional(readOnly = true)
    public List<ResourceCategoryVO> publicCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ResourceCategory>().eq(ResourceCategory::getStatus, 1)
                        .orderByAsc(ResourceCategory::getSortOrder).orderByAsc(ResourceCategory::getId).last("LIMIT 100"))
                .stream().map(this::categoryVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceDiskTypeVO> publicDiskTypes() {
        return diskTypeMapper.selectList(new LambdaQueryWrapper<ResourceDiskType>().eq(ResourceDiskType::getStatus, 1)
                        .orderByAsc(ResourceDiskType::getSortOrder).orderByAsc(ResourceDiskType::getId).last("LIMIT 100"))
                .stream().map(item -> diskVO(item, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceCategoryVO> categoryPage(ResourceQueryDTO query) {
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        Page<ResourceCategory> page = categoryMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), new LambdaQueryWrapper<ResourceCategory>()
                .like(!keyword.isEmpty(), ResourceCategory::getName, keyword)
                .orderByAsc(ResourceCategory::getSortOrder).orderByDesc(ResourceCategory::getId));
        return PageResult.of(page.getRecords().stream().map(this::categoryVO).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public Long createCategory(ResourceCategoryDTO request) {
        ResourceCategory category = new ResourceCategory();
        BeanUtils.copyProperties(request, category);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional
    public void updateCategory(Long id, ResourceCategoryDTO request) {
        if (categoryMapper.selectById(id) == null) throw new ServiceException(404, "分类不存在");
        ResourceCategory category = new ResourceCategory();
        BeanUtils.copyProperties(request, category);
        category.setId(id);
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (resourceMapper.selectCount(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getCategoryId, id)) > 0) {
            throw new ServiceException(409, "分类下仍有资源，不能删除");
        }
        if (categoryMapper.deleteById(id) == 0) throw new ServiceException(404, "分类不存在");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceTagVO> tagPage(ResourceQueryDTO query) {
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        Page<ResourceTag> page = tagMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), new LambdaQueryWrapper<ResourceTag>()
                .like(!keyword.isEmpty(), ResourceTag::getName, keyword).orderByDesc(ResourceTag::getId));
        return PageResult.of(page.getRecords().stream().map(item -> {
            ResourceTagVO vo = new ResourceTagVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public Long createTag(ResourceTagDTO request) {
        ResourceTag tag = new ResourceTag();
        tag.setName(request.getName().trim());
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    @Transactional
    public void updateTag(Long id, ResourceTagDTO request) {
        ResourceTag tag = tagMapper.selectById(id);
        if (tag == null) throw new ServiceException(404, "标签不存在");
        tag.setName(request.getName().trim());
        tagMapper.updateById(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        if (tagRelationMapper.selectCount(new LambdaQueryWrapper<ResourceTagRelation>().eq(ResourceTagRelation::getTagId, id)) > 0) {
            throw new ServiceException(409, "标签仍被资源使用，不能删除");
        }
        if (tagMapper.deleteById(id) == 0) throw new ServiceException(404, "标签不存在");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceDiskTypeVO> diskTypePage(ResourceQueryDTO query) {
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        Page<ResourceDiskType> page = diskTypeMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), new LambdaQueryWrapper<ResourceDiskType>()
                .and(!keyword.isEmpty(), wrapper -> wrapper.like(ResourceDiskType::getName, keyword)
                        .or().like(ResourceDiskType::getCode, keyword))
                .orderByAsc(ResourceDiskType::getSortOrder).orderByDesc(ResourceDiskType::getId));
        return PageResult.of(page.getRecords().stream().map(item -> diskVO(item, true)).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Override
    @Transactional
    public Long createDiskType(ResourceDiskTypeDTO request) {
        ResourceDiskType disk = new ResourceDiskType();
        BeanUtils.copyProperties(request, disk);
        disk.setAllowedHosts(urlValidator.normalizeAllowedHosts(request.getAllowedHosts()));
        diskTypeMapper.insert(disk);
        return disk.getId();
    }

    @Override
    @Transactional
    public void updateDiskType(Long id, ResourceDiskTypeDTO request) {
        if (diskTypeMapper.selectById(id) == null) throw new ServiceException(404, "网盘类型不存在");
        ResourceDiskType disk = new ResourceDiskType();
        BeanUtils.copyProperties(request, disk);
        disk.setId(id);
        disk.setAllowedHosts(urlValidator.normalizeAllowedHosts(request.getAllowedHosts()));
        diskTypeMapper.updateById(disk);
    }

    @Override
    @Transactional
    public void deleteDiskType(Long id) {
        if (linkMapper.selectCount(new LambdaQueryWrapper<ResourceLink>().eq(ResourceLink::getDiskTypeId, id)) > 0) {
            throw new ServiceException(409, "网盘类型仍被链接使用，不能删除");
        }
        if (diskTypeMapper.deleteById(id) == 0) throw new ServiceException(404, "网盘类型不存在");
    }

    private ResourceCategoryVO categoryVO(ResourceCategory category) {
        ResourceCategoryVO vo = new ResourceCategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    private ResourceDiskTypeVO diskVO(ResourceDiskType disk, boolean admin) {
        ResourceDiskTypeVO vo = new ResourceDiskTypeVO();
        BeanUtils.copyProperties(disk, vo);
        if (!admin) vo.setAllowedHosts(null);
        return vo;
    }

    private long pageNo(ResourceQueryDTO query) {
        return Math.max(1, query.getPage());
    }

    private long pageSize(ResourceQueryDTO query) {
        return Math.min(100, Math.max(1, query.getSize()));
    }
}
