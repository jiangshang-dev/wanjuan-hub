package com.heima.modules.operation.controller;

import com.heima.common.R;
import com.heima.modules.operation.service.SiteExperienceService;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayStatusVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.PublicBackgroundMusicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation/site-experience")
@RequiredArgsConstructor
@Tag(name = "公开站点体验配置")
public class PublicSiteExperienceController {
    private final SiteExperienceService siteExperienceService;

    @GetMapping("/gray-day") @Operation(summary = "查询当天是否灰色及原因，供互动模块和前端使用")
    public R<GrayStatusVO> grayDay() {
        return R.ok(siteExperienceService.currentGrayDay());
    }

    @GetMapping("/background-music") @Operation(summary = "查询背景音乐播放器必要配置")
    public R<PublicBackgroundMusicVO> backgroundMusic() {
        return R.ok(siteExperienceService.publicBackgroundMusic());
    }
}
