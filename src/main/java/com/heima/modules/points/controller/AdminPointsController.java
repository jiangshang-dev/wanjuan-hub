package com.heima.modules.points.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.points.dto.PointsDtos.AdjustmentRequest;
import com.heima.modules.points.dto.PointsDtos.LevelRequest;
import com.heima.modules.points.dto.PointsDtos.RuleRequest;
import com.heima.modules.points.entity.PointsEntities.Level;
import com.heima.modules.points.entity.PointsEntities.Rule;
import com.heima.modules.points.service.PointsService;
import com.heima.modules.points.vo.PointsVOs.RecordVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
@Tag(name = "后台积分与等级")
public class AdminPointsController {
    private final PointsService pointsService;

    @GetMapping("/rules")
    @Operation(summary = "积分规则分页")
    @PreAuthorize("hasAuthority('points:rule:list')")
    public R<PageResult<Rule>> rules(@RequestParam(defaultValue = "1") @Min(1) long page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return R.ok(pointsService.rules(page, size));
    }

    @PostMapping("/rules")
    @Operation(summary = "新增积分规则")
    @PreAuthorize("hasAuthority('points:rule:create')")
    public R<Long> createRule(@Valid @RequestBody RuleRequest request) {
        return R.ok(pointsService.createRule(request));
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "修改积分规则")
    @PreAuthorize("hasAuthority('points:rule:update')")
    public R<Void> updateRule(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
        pointsService.updateRule(id, request);
        return R.ok();
    }

    @GetMapping("/levels")
    @Operation(summary = "全部等级配置")
    @PreAuthorize("hasAuthority('points:level:list')")
    public R<List<Level>> levels() {
        return R.ok(pointsService.adminLevels());
    }

    @PostMapping("/levels")
    @Operation(summary = "新增等级")
    @PreAuthorize("hasAuthority('points:level:create')")
    public R<Long> createLevel(@Valid @RequestBody LevelRequest request) {
        return R.ok(pointsService.createLevel(request));
    }

    @PutMapping("/levels/{id}")
    @Operation(summary = "修改等级")
    @PreAuthorize("hasAuthority('points:level:update')")
    public R<Void> updateLevel(@PathVariable Long id, @Valid @RequestBody LevelRequest request) {
        pointsService.updateLevel(id, request);
        return R.ok();
    }

    @GetMapping("/adjustments")
    @Operation(summary = "积分调整与全部流水")
    @PreAuthorize("hasAuthority('points:adjustment:list')")
    public R<PageResult<RecordVO>> adjustments(@RequestParam(defaultValue = "1") @Min(1) long page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return R.ok(pointsService.adminRecords(page, size));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "幂等调整用户积分与经验")
    @PreAuthorize("hasAuthority('points:adjustment:create')")
    public R<Boolean> adjust(@Valid @RequestBody AdjustmentRequest request) {
        return R.ok(pointsService.adjust(request, UserUtil.requireAdmin().getId()));
    }
}
