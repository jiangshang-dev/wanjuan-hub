package com.heima.modules.resource.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.service.ResourceAccessService;
import com.heima.modules.resource.service.ResourceCatalogService;
import com.heima.modules.resource.service.ResourceService;
import com.heima.modules.resource.vo.ResourceAttachmentVO;
import com.heima.modules.resource.vo.ResourceCategoryVO;
import com.heima.modules.resource.vo.ResourceDiskTypeVO;
import com.heima.modules.resource.vo.ResourceLinkVO;
import com.heima.modules.resource.vo.ResourceVO;
import com.heima.security.context.LoginUser;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "公开资源")
public class PublicResourceController {
    private final ResourceService resourceService;
    private final ResourceCatalogService catalogService;
    private final ResourceAccessService accessService;

    @GetMapping("/api/resources")
    @Operation(summary = "分页查询当前公开版本，只搜索公开文本")
    public R<PageResult<ResourceVO>> page(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(resourceService.publicPage(query));
    }

    @GetMapping("/api/resources/{id}")
    @Operation(summary = "查看当前公开版本及访问权益状态")
    public R<ResourceVO> detail(@PathVariable Long id) {
        LoginUser user = UserUtil.getUser();
        return R.ok(resourceService.publicDetail(id, user == null ? null : user.getId(), user != null && user.isAdmin()));
    }

    @GetMapping("/api/resources/{id}/links")
    @Operation(summary = "统一授权后获取解密网盘链接")
    public R<List<ResourceLinkVO>> links(@PathVariable Long id) {
        LoginUser user = UserUtil.getUser();
        return R.ok(accessService.links(id, user == null ? null : user.getId(), user != null && user.isAdmin()));
    }

    @GetMapping("/api/resources/{id}/attachments/{fileId}")
    @Operation(summary = "统一授权后获取私有附件临时地址")
    public R<ResourceAttachmentVO> attachment(@PathVariable Long id, @PathVariable Long fileId) {
        LoginUser user = UserUtil.getUser();
        return R.ok(accessService.attachment(id, fileId, user == null ? null : user.getId(), user != null && user.isAdmin()));
    }

    @GetMapping("/api/resource-categories")
    @Operation(summary = "查询启用资源分类，最多100条")
    public R<List<ResourceCategoryVO>> categories() {
        return R.ok(catalogService.publicCategories());
    }

    @GetMapping("/api/resource-disk-types")
    @Operation(summary = "查询启用网盘类型，不公开域名白名单")
    public R<List<ResourceDiskTypeVO>> diskTypes() {
        return R.ok(catalogService.publicDiskTypes());
    }
}
