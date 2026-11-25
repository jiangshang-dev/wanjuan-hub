package com.heima.modules.points.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.modules.points.service.PointsService;
import com.heima.modules.points.vo.PointsVOs.AccountVO;
import com.heima.modules.points.vo.PointsVOs.LevelVO;
import com.heima.modules.points.vo.PointsVOs.MembershipVO;
import com.heima.modules.points.vo.PointsVOs.RecordVO;
import com.heima.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "用户积分与等级")
public class UserPointsController {
    private final PointsService pointsService;

    @PostMapping("/api/user/sign-in")
    @Operation(summary = "每日签到")
    public R<AccountVO> signIn() {
        return R.ok(pointsService.signIn(UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/api/user/points")
    @Operation(summary = "我的积分账户")
    public R<AccountVO> account() {
        return R.ok(pointsService.account(UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/api/user/points/records")
    @Operation(summary = "我的积分流水")
    public R<PageResult<RecordVO>> records(@RequestParam(defaultValue = "1") @Min(1) long page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return R.ok(pointsService.records(UserUtil.requireFrontUser().getId(), page, size));
    }

    @GetMapping("/api/user/membership")
    @Operation(summary = "我的会员等级")
    public R<MembershipVO> membership() {
        return R.ok(pointsService.membership(UserUtil.requireFrontUser().getId()));
    }

    @GetMapping("/api/user/levels")
    @Operation(summary = "启用等级列表")
    public R<List<LevelVO>> levels() {
        return R.ok(pointsService.levels());
    }
}
