package com.heima.modules.operation.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.operation.dto.OperationDtos.AdvertisementRequest;
import com.heima.modules.operation.dto.OperationDtos.AnnouncementRequest;
import com.heima.modules.operation.dto.OperationDtos.ConfigRequest;
import com.heima.modules.operation.dto.OperationDtos.FriendLinkRequest;
import com.heima.modules.operation.dto.OperationDtos.HotKeywordRequest;
import com.heima.modules.operation.dto.OperationDtos.RecommendationRequest;
import com.heima.modules.operation.dto.OperationDtos.StatusRequest;
import com.heima.modules.operation.service.OperationService;
import com.heima.modules.operation.vo.OperationVOs.AdvertisementVO;
import com.heima.modules.operation.vo.OperationVOs.AnnouncementVO;
import com.heima.modules.operation.vo.OperationVOs.ConfigVO;
import com.heima.modules.operation.vo.OperationVOs.FriendLinkVO;
import com.heima.modules.operation.vo.OperationVOs.HotKeywordVO;
import com.heima.modules.operation.vo.OperationVOs.RecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/operation")
@RequiredArgsConstructor
@Tag(name = "后台运营管理")
public class AdminOperationController {
    private final OperationService operationService;

    @GetMapping("/announcements") @Operation(summary = "公告分页")
    @PreAuthorize("hasAuthority('operation:announcement:list')")
    public R<PageResult<AnnouncementVO>> announcements(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(operationService.announcementPage(page, size, keyword, status));
    }

    @PostMapping("/announcements") @Operation(summary = "新增公告")
    @OperationLog(module = "运营公告", value = "新增公告") @PreAuthorize("hasAuthority('operation:announcement:create')")
    public R<String> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return R.ok(operationService.createAnnouncement(request));
    }

    @PutMapping("/announcements/{id}") @Operation(summary = "修改公告")
    @OperationLog(module = "运营公告", value = "修改公告", targetId = "#id") @PreAuthorize("hasAuthority('operation:announcement:update')")
    public R<Void> updateAnnouncement(@PathVariable @Positive Long id, @Valid @RequestBody AnnouncementRequest request) {
        operationService.updateAnnouncement(id, request); return R.ok();
    }

    @PutMapping("/announcements/{id}/status") @Operation(summary = "启停公告")
    @OperationLog(module = "运营公告", value = "启停公告", targetId = "#id") @PreAuthorize("hasAuthority('operation:announcement:status')")
    public R<Void> announcementStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        operationService.announcementStatus(id, request.getStatus()); return R.ok();
    }

    @DeleteMapping("/announcements/{id}") @Operation(summary = "删除公告")
    @OperationLog(module = "运营公告", value = "删除公告", targetId = "#id") @PreAuthorize("hasAuthority('operation:announcement:delete')")
    public R<Void> deleteAnnouncement(@PathVariable @Positive Long id) {
        operationService.deleteAnnouncement(id); return R.ok();
    }

    @GetMapping("/recommendations") @Operation(summary = "推荐位分页")
    @PreAuthorize("hasAuthority('operation:recommendation:list')")
    public R<PageResult<RecommendationVO>> recommendations(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 24) String contentType,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(operationService.recommendationPage(page, size, keyword, contentType, status));
    }

    @PostMapping("/recommendations") @Operation(summary = "新增推荐位")
    @OperationLog(module = "推荐位", value = "新增推荐位") @PreAuthorize("hasAuthority('operation:recommendation:create')")
    public R<String> createRecommendation(@Valid @RequestBody RecommendationRequest request) {
        return R.ok(operationService.createRecommendation(request));
    }

    @PutMapping("/recommendations/{id}") @Operation(summary = "修改推荐位")
    @OperationLog(module = "推荐位", value = "修改推荐位", targetId = "#id") @PreAuthorize("hasAuthority('operation:recommendation:update')")
    public R<Void> updateRecommendation(@PathVariable @Positive Long id, @Valid @RequestBody RecommendationRequest request) {
        operationService.updateRecommendation(id, request); return R.ok();
    }

    @PutMapping("/recommendations/{id}/status") @Operation(summary = "启停推荐位")
    @OperationLog(module = "推荐位", value = "启停推荐位", targetId = "#id") @PreAuthorize("hasAuthority('operation:recommendation:status')")
    public R<Void> recommendationStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        operationService.recommendationStatus(id, request.getStatus()); return R.ok();
    }

    @DeleteMapping("/recommendations/{id}") @Operation(summary = "删除推荐位")
    @OperationLog(module = "推荐位", value = "删除推荐位", targetId = "#id") @PreAuthorize("hasAuthority('operation:recommendation:delete')")
    public R<Void> deleteRecommendation(@PathVariable @Positive Long id) {
        operationService.deleteRecommendation(id); return R.ok();
    }

    @GetMapping("/advertisements") @Operation(summary = "广告位分页")
    @PreAuthorize("hasAuthority('operation:ad:list')")
    public R<PageResult<AdvertisementVO>> advertisements(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 40) String position,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(operationService.advertisementPage(page, size, keyword, position, status));
    }

    @PostMapping("/advertisements") @Operation(summary = "新增广告位")
    @OperationLog(module = "广告位", value = "新增广告位") @PreAuthorize("hasAuthority('operation:ad:create')")
    public R<String> createAdvertisement(@Valid @RequestBody AdvertisementRequest request) {
        return R.ok(operationService.createAdvertisement(request));
    }

    @PutMapping("/advertisements/{id}") @Operation(summary = "修改广告位")
    @OperationLog(module = "广告位", value = "修改广告位", targetId = "#id") @PreAuthorize("hasAuthority('operation:ad:update')")
    public R<Void> updateAdvertisement(@PathVariable @Positive Long id, @Valid @RequestBody AdvertisementRequest request) {
        operationService.updateAdvertisement(id, request); return R.ok();
    }

    @PutMapping("/advertisements/{id}/status") @Operation(summary = "启停广告位")
    @OperationLog(module = "广告位", value = "启停广告位", targetId = "#id") @PreAuthorize("hasAuthority('operation:ad:status')")
    public R<Void> advertisementStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        operationService.advertisementStatus(id, request.getStatus()); return R.ok();
    }

    @DeleteMapping("/advertisements/{id}") @Operation(summary = "删除广告位")
    @OperationLog(module = "广告位", value = "删除广告位", targetId = "#id") @PreAuthorize("hasAuthority('operation:ad:delete')")
    public R<Void> deleteAdvertisement(@PathVariable @Positive Long id) {
        operationService.deleteAdvertisement(id); return R.ok();
    }

    @GetMapping("/friend-links") @Operation(summary = "友情链接分页")
    @PreAuthorize("hasAuthority('operation:link:list')")
    public R<PageResult<FriendLinkVO>> friendLinks(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(operationService.friendLinkPage(page, size, keyword, status));
    }

    @PostMapping("/friend-links") @Operation(summary = "新增友情链接")
    @OperationLog(module = "友情链接", value = "新增友情链接") @PreAuthorize("hasAuthority('operation:link:create')")
    public R<String> createFriendLink(@Valid @RequestBody FriendLinkRequest request) {
        return R.ok(operationService.createFriendLink(request));
    }

    @PutMapping("/friend-links/{id}") @Operation(summary = "修改友情链接")
    @OperationLog(module = "友情链接", value = "修改友情链接", targetId = "#id") @PreAuthorize("hasAuthority('operation:link:update')")
    public R<Void> updateFriendLink(@PathVariable @Positive Long id, @Valid @RequestBody FriendLinkRequest request) {
        operationService.updateFriendLink(id, request); return R.ok();
    }

    @PutMapping("/friend-links/{id}/status") @Operation(summary = "启停友情链接")
    @OperationLog(module = "友情链接", value = "启停友情链接", targetId = "#id") @PreAuthorize("hasAuthority('operation:link:status')")
    public R<Void> friendLinkStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        operationService.friendLinkStatus(id, request.getStatus()); return R.ok();
    }

    @DeleteMapping("/friend-links/{id}") @Operation(summary = "删除友情链接")
    @OperationLog(module = "友情链接", value = "删除友情链接", targetId = "#id") @PreAuthorize("hasAuthority('operation:link:delete')")
    public R<Void> deleteFriendLink(@PathVariable @Positive Long id) {
        operationService.deleteFriendLink(id); return R.ok();
    }

    @GetMapping("/hot-searches") @Operation(summary = "热门搜索分页")
    @PreAuthorize("hasAuthority('operation:hot-search:list')")
    public R<PageResult<HotKeywordVO>> hotKeywords(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(operationService.hotKeywordPage(page, size, keyword, status));
    }

    @PostMapping("/hot-searches") @Operation(summary = "新增热门搜索")
    @OperationLog(module = "热门搜索", value = "新增热门搜索") @PreAuthorize("hasAuthority('operation:hot-search:create')")
    public R<String> createHotKeyword(@Valid @RequestBody HotKeywordRequest request) {
        return R.ok(operationService.createHotKeyword(request));
    }

    @PutMapping("/hot-searches/{id}") @Operation(summary = "修改热门搜索")
    @OperationLog(module = "热门搜索", value = "修改热门搜索", targetId = "#id") @PreAuthorize("hasAuthority('operation:hot-search:update')")
    public R<Void> updateHotKeyword(@PathVariable @Positive Long id, @Valid @RequestBody HotKeywordRequest request) {
        operationService.updateHotKeyword(id, request); return R.ok();
    }

    @PutMapping("/hot-searches/{id}/status") @Operation(summary = "启停热门搜索")
    @OperationLog(module = "热门搜索", value = "启停热门搜索", targetId = "#id") @PreAuthorize("hasAuthority('operation:hot-search:status')")
    public R<Void> hotKeywordStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        operationService.hotKeywordStatus(id, request.getStatus()); return R.ok();
    }

    @DeleteMapping("/hot-searches/{id}") @Operation(summary = "删除热门搜索")
    @OperationLog(module = "热门搜索", value = "删除热门搜索", targetId = "#id") @PreAuthorize("hasAuthority('operation:hot-search:delete')")
    public R<Void> deleteHotKeyword(@PathVariable @Positive Long id) {
        operationService.deleteHotKeyword(id); return R.ok();
    }

    @GetMapping("/configs") @Operation(summary = "网站参数分页")
    @PreAuthorize("hasAuthority('operation:config:list')")
    public R<PageResult<ConfigVO>> configs(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer publicFlag,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return R.ok(operationService.configPage(page, size, keyword, publicFlag, status));
    }

    @PostMapping("/configs") @Operation(summary = "新增网站参数")
    @OperationLog(module = "网站参数", value = "新增网站参数") @PreAuthorize("hasAuthority('operation:config:create')")
    public R<String> createConfig(@Valid @RequestBody ConfigRequest request) {
        return R.ok(operationService.createConfig(request));
    }

    @PutMapping("/configs/{id}") @Operation(summary = "修改网站参数")
    @OperationLog(module = "网站参数", value = "修改网站参数", targetId = "#id") @PreAuthorize("hasAuthority('operation:config:update')")
    public R<Void> updateConfig(@PathVariable @Positive Long id, @Valid @RequestBody ConfigRequest request) {
        operationService.updateConfig(id, request); return R.ok();
    }

    @PutMapping("/configs/{id}/status") @Operation(summary = "启停网站参数")
    @OperationLog(module = "网站参数", value = "启停网站参数", targetId = "#id") @PreAuthorize("hasAuthority('operation:config:status')")
    public R<Void> configStatus(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        operationService.configStatus(id, request.getStatus()); return R.ok();
    }

    @DeleteMapping("/configs/{id}") @Operation(summary = "删除网站参数")
    @OperationLog(module = "网站参数", value = "删除网站参数", targetId = "#id") @PreAuthorize("hasAuthority('operation:config:delete')")
    public R<Void> deleteConfig(@PathVariable @Positive Long id) {
        operationService.deleteConfig(id); return R.ok();
    }
}
