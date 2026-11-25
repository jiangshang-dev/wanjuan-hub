package com.heima.modules.resource.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.annotation.OperationLog;
import com.heima.common.R;
import com.heima.modules.resource.dto.AdminResourceStatusDTO;
import com.heima.modules.resource.dto.ResourceInvalidReportHandleDTO;
import com.heima.modules.resource.dto.ResourceQueryDTO;
import com.heima.modules.resource.dto.ResourceReportQueryDTO;
import com.heima.modules.resource.service.ResourceReportService;
import com.heima.modules.resource.service.ResourceService;
import com.heima.modules.resource.vo.ResourceInvalidReportVO;
import com.heima.modules.resource.vo.ResourceVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "后台资源管理")
@PreAuthorize("hasRole('ADMIN')")
public class AdminResourceController {
    private final ResourceService resourceService;
    private final ResourceReportService reportService;

    @GetMapping("/api/admin/resources")
    @Operation(summary = "分页查询资源最新版本")
    @PreAuthorize("hasAuthority('resource:list')")
    public R<PageResult<ResourceVO>> page(@Valid @ModelAttribute ResourceQueryDTO query) {
        return R.ok(resourceService.adminPage(query));
    }

    @PutMapping("/api/admin/resources/{id}/status")
    @Operation(summary = "恢复公开、下线或标记资源失效")
    @OperationLog(module = "资源管理", value = "修改资源状态", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:status')")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody AdminResourceStatusDTO request) {
        resourceService.adminStatus(id, request);
        return R.ok();
    }

    @GetMapping("/api/admin/resource-invalid-reports")
    @Operation(summary = "分页查询资源失效举报")
    @PreAuthorize("hasAuthority('resource:report:list')")
    public R<PageResult<ResourceInvalidReportVO>> reports(@Valid @ModelAttribute ResourceReportQueryDTO query) {
        return R.ok(reportService.page(query));
    }

    @PutMapping("/api/admin/resource-invalid-reports/{id}/handle")
    @Operation(summary = "处理失效举报，确认后禁用链接并下线资源")
    @OperationLog(module = "资源管理", value = "处理资源失效举报", targetId = "#id")
    @PreAuthorize("hasAuthority('resource:report:handle')")
    public R<Void> handleReport(@PathVariable Long id, @Valid @RequestBody ResourceInvalidReportHandleDTO request) {
        reportService.handle(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }
}
