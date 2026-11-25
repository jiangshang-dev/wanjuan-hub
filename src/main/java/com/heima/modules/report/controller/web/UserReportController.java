package com.heima.modules.report.controller.web;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.report.dto.ReportDtos.CreateRequest;
import com.heima.modules.report.dto.ReportDtos.Query;
import com.heima.modules.report.service.ReportService;
import com.heima.modules.report.vo.ReportVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/reports")
@RequiredArgsConstructor
@Tag(name = "用户内容举报")
public class UserReportController {
    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "举报公开内容")
    public R<Long> create(@Valid @RequestBody CreateRequest request) {
        return R.ok(reportService.create(request, UserUtil.requireFrontUser().getId()));
    }

    @GetMapping
    @Operation(summary = "分页查询自己的举报及处理结果")
    public R<PageResult<ReportVO>> page(@Valid @ModelAttribute Query query) {
        return R.ok(reportService.userPage(query, UserUtil.requireFrontUser().getId()));
    }
}
