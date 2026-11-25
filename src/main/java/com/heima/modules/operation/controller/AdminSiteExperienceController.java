package com.heima.modules.operation.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.operation.dto.OperationDtos.StatusRequest;
import com.heima.modules.operation.dto.SiteExperienceDtos.BackgroundMusicRequest;
import com.heima.modules.operation.dto.SiteExperienceDtos.GrayDayRequest;
import com.heima.modules.operation.service.SiteExperienceService;
import com.heima.modules.operation.vo.SiteExperienceVOs.BackgroundMusicVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayDayVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/admin/operation/site-experience")
@RequiredArgsConstructor
@Tag(name = "后台站点体验配置")
public class AdminSiteExperienceController {
    private final SiteExperienceService siteExperienceService;

    @GetMapping("/gray-days") @Operation(summary = "特殊灰色日分页")
    @PreAuthorize("hasAuthority('operation:gray-day:list')")
    public R<PageResult<GrayDayVO>> grayDays(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "开始日期，yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "结束日期，yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(siteExperienceService.grayDayPage(page, size, startDate, endDate, status));
    }

    @PostMapping("/gray-days") @Operation(summary = "新增特殊灰色日")
    @OperationLog(module = "特殊灰色日", value = "新增灰色日")
    @PreAuthorize("hasAuthority('operation:gray-day:create')")
    public R<String> createGrayDay(@Valid @RequestBody GrayDayRequest request) {
        return R.ok(siteExperienceService.createGrayDay(request));
    }

    @PutMapping("/gray-days/{id}") @Operation(summary = "修改特殊灰色日")
    @OperationLog(module = "特殊灰色日", value = "修改灰色日", targetId = "#id")
    @PreAuthorize("hasAuthority('operation:gray-day:update')")
    public R<Void> updateGrayDay(@PathVariable @Positive Long id, @Valid @RequestBody GrayDayRequest request) {
        siteExperienceService.updateGrayDay(id, request);
        return R.ok();
    }

    @PutMapping("/gray-days/{id}/status") @Operation(summary = "启停特殊灰色日")
    @OperationLog(module = "特殊灰色日", value = "启停灰色日", targetId = "#id")
    @PreAuthorize("hasAuthority('operation:gray-day:status')")
    public R<Void> grayDayStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        siteExperienceService.grayDayStatus(id, request.getStatus());
        return R.ok();
    }

    @GetMapping("/background-music") @Operation(summary = "查询背景音乐配置")
    @PreAuthorize("hasAuthority('operation:music:list')")
    public R<BackgroundMusicVO> backgroundMusic() {
        return R.ok(siteExperienceService.backgroundMusic());
    }

    @PutMapping("/background-music") @Operation(summary = "保存背景音乐开关、音频、播放方式及播放器显隐")
    @OperationLog(module = "背景音乐", value = "保存背景音乐配置")
    @PreAuthorize("hasAuthority('operation:music:update')")
    public R<Void> saveBackgroundMusic(@Valid @RequestBody BackgroundMusicRequest request) {
        siteExperienceService.saveBackgroundMusic(request);
        return R.ok();
    }
}
