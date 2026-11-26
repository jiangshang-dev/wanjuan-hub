package com.heima.modules.search.controller;

import com.heima.common.R;
import com.heima.modules.search.dto.SearchDtos.SearchCompensationVO;
import com.heima.modules.search.dto.SearchDtos.SearchRebuildVO;
import com.heima.modules.search.service.SearchMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/search/index")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "后台搜索索引维护")
public class AdminSearchIndexController {
    private final SearchMaintenanceService searchMaintenanceService;

    @PostMapping("/{contentType}/{contentId}")
    @Operation(summary = "补偿单条博客或论坛主题索引", description = "在事务中写入一条新版本的outbox事件，由MQ异步重读MySQL当前公开状态并更新或删除ES文档")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "补偿事件已写入"),
            @ApiResponse(responseCode = "400", description = "内容类型或ID不支持"),
            @ApiResponse(responseCode = "403", description = "无后台管理权限")})
    public R<SearchCompensationVO> compensate(
            @Parameter(description = "内容类型：BLOG或FORUM_TOPIC", required = true) @PathVariable String contentType,
            @Parameter(description = "内容ID", required = true) @PathVariable @Positive Long contentId) {
        return R.ok(searchMaintenanceService.compensate(contentType, contentId));
    }

    @PostMapping("/rebuild")
    @Operation(summary = "全量重建博客和论坛主题索引", description = "为全部博客和主题（含删除、下线记录）写入新版本outbox事件，异步补建公开文档并清理陈旧文档")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "全量重建事件已写入"),
            @ApiResponse(responseCode = "403", description = "无后台管理权限")})
    public R<SearchRebuildVO> rebuildAll() {
        return R.ok(searchMaintenanceService.rebuildAll());
    }
}
