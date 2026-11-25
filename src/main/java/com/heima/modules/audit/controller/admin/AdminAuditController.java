package com.heima.modules.audit.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.audit.dto.AuditQueryDTO;
import com.heima.modules.audit.dto.ManualReviewDTO;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.audit.vo.AuditDetailVO;
import com.heima.modules.audit.vo.AuditTaskVO;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/audits")
@RequiredArgsConstructor
@Tag(name = "后台审核中心")
public class AdminAuditController {
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "分页查询审核任务")
    @PreAuthorize("hasAuthority('audit:task:list')")
    public R<PageResult<AuditTaskVO>> page(@Valid @ModelAttribute AuditQueryDTO query) {
        return R.ok(auditService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "审核任务详情")
    @PreAuthorize("hasAuthority('audit:task:detail')")
    public R<AuditDetailVO> detail(@PathVariable Long id) {
        return R.ok(auditService.detail(id));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重试审核任务")
    @PreAuthorize("hasAuthority('audit:task:retry')")
    public R<Void> retry(@PathVariable Long id) {
        auditService.retry(id);
        return R.ok();
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "人工通过或驳回")
    @PreAuthorize("hasAuthority('audit:task:review')")
    public R<Void> review(@PathVariable Long id, @Valid @RequestBody ManualReviewDTO request) {
        auditService.review(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }
}
