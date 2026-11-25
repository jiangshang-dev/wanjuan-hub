package com.heima.modules.operation.controller;

import com.heima.common.R;
import com.heima.modules.operation.service.OperationService;
import com.heima.modules.operation.vo.OperationVOs.AdvertisementVO;
import com.heima.modules.operation.vo.OperationVOs.FriendLinkVO;
import com.heima.modules.operation.vo.OperationVOs.HotKeywordVO;
import com.heima.modules.operation.vo.OperationVOs.PublicConfigVO;
import com.heima.modules.operation.vo.OperationVOs.RecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/operation")
@RequiredArgsConstructor
@Tag(name = "公开运营配置")
public class PublicOperationController {
    private final OperationService operationService;

    @GetMapping("/recommendations") @Operation(summary = "生效且指向公开内容的推荐位")
    public R<List<RecommendationVO>> recommendations(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return R.ok(operationService.publicRecommendations(limit));
    }

    @GetMapping("/advertisements") @Operation(summary = "当前时间可投放的广告")
    public R<List<AdvertisementVO>> advertisements(@RequestParam(required = false) @Size(max = 40) String position,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return R.ok(operationService.publicAdvertisements(position, limit));
    }

    @GetMapping("/friend-links") @Operation(summary = "启用的友情链接")
    public R<List<FriendLinkVO>> friendLinks(@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return R.ok(operationService.publicFriendLinks(limit));
    }

    @GetMapping("/hot-searches") @Operation(summary = "启用的热门搜索词及热度")
    public R<List<HotKeywordVO>> hotKeywords(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return R.ok(operationService.publicHotKeywords(limit));
    }

    @GetMapping("/configs") @Operation(summary = "公开网站参数、协议、隐私政策及备案信息")
    public R<List<PublicConfigVO>> configs(@RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit) {
        return R.ok(operationService.publicConfigs(limit));
    }
}
