package com.heima.modules.statistics.controller;

import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.statistics.service.StatisticsService;
import com.heima.modules.statistics.vo.StatisticsVOs.DailyStatisticsVO;
import com.heima.modules.statistics.vo.StatisticsVOs.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "后台统计工作台")
public class AdminStatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "实时汇总、趋势、公开内容分类及MQ/Outbox积压")
    @PreAuthorize("hasAuthority('statistics:dashboard:view')")
    public R<DashboardVO> dashboard(@RequestParam(defaultValue = "7") @Min(1) @Max(31) int days) {
        return R.ok(statisticsService.dashboard(days));
    }

    @PostMapping("/daily/refresh")
    @Operation(summary = "幂等重算指定日期汇总，默认昨天，允许重算当天部分数据")
    @OperationLog(module = "统计工作台", value = "重算每日汇总")
    @PreAuthorize("hasAuthority('statistics:dashboard:refresh')")
    public R<DailyStatisticsVO> refreshDaily(@RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        return R.ok(statisticsService.refreshDaily(statDate));
    }
}
