package com.heima.modules.resource.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.resource.dto.ResourceEntitlementQueryDTO;
import com.heima.modules.resource.service.ResourceEntitlementService;
import com.heima.modules.resource.vo.ResourceEntitlementAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/resource-entitlements")
@RequiredArgsConstructor
@Tag(name = "后台资源授权查询")
public class AdminResourceEntitlementController {
    private final ResourceEntitlementService entitlementService;

    @GetMapping
    @Operation(summary = "分页查询资源授权，支持状态、用户、资源和来源订单筛选")
    @PreAuthorize("hasAuthority('resource:entitlement:list')")
    public R<PageResult<ResourceEntitlementAdminVO>> page(@Valid @ModelAttribute ResourceEntitlementQueryDTO query) {
        return R.ok(entitlementService.adminPage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看资源授权详情")
    @PreAuthorize("hasAuthority('resource:entitlement:list')")
    public R<ResourceEntitlementAdminVO> detail(@PathVariable Long id) {
        return R.ok(entitlementService.adminDetail(id));
    }
}
