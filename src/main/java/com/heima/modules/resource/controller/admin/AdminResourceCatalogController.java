package com.heima.modules.resource.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.annotation.OperationLog;
import com.heima.common.R;
import com.heima.modules.resource.dto.ResourceCategoryDTO;
import com.heima.modules.resource.dto.ResourceDiskTypeDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceTagDTO;
import com.heima.modules.resource.service.ResourceCatalogService;
import com.heima.modules.resource.vo.ResourceCategoryVO;
import com.heima.modules.resource.vo.ResourceDiskTypeVO;
import com.heima.modules.resource.vo.ResourceTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "后台资源目录管理")
@PreAuthorize("hasRole('ADMIN')")
public class AdminResourceCatalogController {
    private final ResourceCatalogService catalogService;

    @GetMapping("/api/admin/resource-categories")
    @Operation(summary = "分页查询资源分类")
    @PreAuthorize("hasAuthority('resource:category:list')")
    public R<PageResult<ResourceCategoryVO>> categories(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(catalogService.categoryPage(query));
    }

    @PostMapping("/api/admin/resource-categories")
    @Operation(summary = "创建资源分类")
    @OperationLog(module = "资源目录", value = "创建资源分类")
    @PreAuthorize("hasAuthority('resource:category:create')")
    public R<Long> createCategory(@Valid @RequestBody ResourceCategoryDTO request) {
        return R.ok(catalogService.createCategory(request));
    }

    @PutMapping("/api/admin/resource-categories/{id}")
    @Operation(summary = "修改资源分类")
    @OperationLog(module = "资源目录", value = "修改资源分类", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:category:update')")
    public R<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody ResourceCategoryDTO request) {
        catalogService.updateCategory(id, request);
        return R.ok();
    }

    @DeleteMapping("/api/admin/resource-categories/{id}")
    @Operation(summary = "删除未被资源使用的分类")
    @OperationLog(module = "资源目录", value = "删除资源分类", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:category:delete')")
    public R<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return R.ok();
    }

    @GetMapping("/api/admin/resource-tags")
    @Operation(summary = "分页查询资源标签")
    @PreAuthorize("hasAuthority('resource:tag:list')")
    public R<PageResult<ResourceTagVO>> tags(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(catalogService.tagPage(query));
    }

    @PostMapping("/api/admin/resource-tags")
    @Operation(summary = "创建资源标签")
    @OperationLog(module = "资源目录", value = "创建资源标签")
    @PreAuthorize("hasAuthority('resource:tag:create')")
    public R<Long> createTag(@Valid @RequestBody ResourceTagDTO request) {
        return R.ok(catalogService.createTag(request));
    }

    @PutMapping("/api/admin/resource-tags/{id}")
    @Operation(summary = "修改资源标签")
    @OperationLog(module = "资源目录", value = "修改资源标签", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:tag:update')")
    public R<Void> updateTag(@PathVariable Long id, @Valid @RequestBody ResourceTagDTO request) {
        catalogService.updateTag(id, request);
        return R.ok();
    }

    @DeleteMapping("/api/admin/resource-tags/{id}")
    @Operation(summary = "删除未被资源使用的标签")
    @OperationLog(module = "资源目录", value = "删除资源标签", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:tag:delete')")
    public R<Void> deleteTag(@PathVariable Long id) {
        catalogService.deleteTag(id);
        return R.ok();
    }

    @GetMapping("/api/admin/resource-disk-types")
    @Operation(summary = "分页查询网盘类型和域名白名单")
    @PreAuthorize("hasAuthority('resource:disk-type:list')")
    public R<PageResult<ResourceDiskTypeVO>> diskTypes(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(catalogService.diskTypePage(query));
    }

    @PostMapping("/api/admin/resource-disk-types")
    @Operation(summary = "创建网盘类型及域名白名单")
    @OperationLog(module = "资源目录", value = "创建网盘类型")
    @PreAuthorize("hasAuthority('resource:disk-type:create')")
    public R<Long> createDiskType(@Valid @RequestBody ResourceDiskTypeDTO request) {
        return R.ok(catalogService.createDiskType(request));
    }

    @PutMapping("/api/admin/resource-disk-types/{id}")
    @Operation(summary = "修改网盘类型及域名白名单")
    @OperationLog(module = "资源目录", value = "修改网盘类型", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:disk-type:update')")
    public R<Void> updateDiskType(@PathVariable Long id, @Valid @RequestBody ResourceDiskTypeDTO request) {
        catalogService.updateDiskType(id, request);
        return R.ok();
    }

    @DeleteMapping("/api/admin/resource-disk-types/{id}")
    @Operation(summary = "删除未被链接使用的网盘类型")
    @OperationLog(module = "资源目录", value = "删除网盘类型", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:disk-type:delete')")
    public R<Void> deleteDiskType(@PathVariable Long id) {
        catalogService.deleteDiskType(id);
        return R.ok();
    }
}
