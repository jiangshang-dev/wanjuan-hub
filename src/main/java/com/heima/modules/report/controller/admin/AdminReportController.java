package com.heima.modules.report.controller.admin;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.report.dto.ReportDtos.HandleRequest;
import com.heima.modules.report.dto.ReportDtos.Query;
import com.heima.modules.report.service.ReportService;
import com.heima.modules.report.vo.ReportVO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "后台内容举报处理")
public class AdminReportController {
    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("hasAuthority('report:manage:list')")
    @Operation(summary = "分页查询举报及处理记录")
    public R<PageResult<ReportVO>> page(@Valid @ModelAttribute Query query) { return R.ok(reportService.adminPage(query)); }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('report:manage:handle')")
    @Operation(summary = "下线违规内容、保留内容或驳回举报")
    public R<Void> handle(@PathVariable Long id, @Valid @RequestBody HandleRequest request) {
        reportService.handle(id, request, UserUtil.requireAdmin().getId());
        return R.ok();
    }
}
