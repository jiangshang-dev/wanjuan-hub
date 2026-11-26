package com.heima.modules.membership.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.membership.dto.MembershipDtos.CommissionConfigRequest;
import com.heima.modules.membership.dto.MembershipDtos.PlanQuery;
import com.heima.modules.membership.dto.MembershipDtos.PlanRequest;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.membership.vo.MembershipVOs.CommissionConfigVO;
import com.heima.modules.membership.vo.MembershipVOs.PlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "后台会员与返佣配置")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMembershipController {
    private final MembershipService membershipService;

    @GetMapping("/api/admin/membership-plans")
    @Operation(summary = "分页查询会员套餐")
    @PreAuthorize("hasAuthority('membership:plan:list')")
    public R<PageResult<PlanVO>> plans(@Valid @ModelAttribute PlanQuery query) {
        return R.ok(membershipService.adminPlans(query));
    }

    @PostMapping("/api/admin/membership-plans")
    @Operation(summary = "新增付费会员套餐")
    @OperationLog(module = "会员管理", value = "新增会员套餐")
    @PreAuthorize("hasAuthority('membership:plan:create')")
    public R<Long> create(@Valid @RequestBody PlanRequest request) {
        return R.ok(membershipService.createPlan(request));
    }

    @PutMapping("/api/admin/membership-plans/{id}")
    @Operation(summary = "修改套餐价格、有效期、每日领取数量及启停状态")
    @OperationLog(module = "会员管理", value = "修改会员套餐", targetId = "#id")
    @PreAuthorize("hasAuthority('membership:plan:update')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        membershipService.updatePlan(id, request);
        return R.ok();
    }

    @GetMapping("/api/admin/membership/commission-config")
    @Operation(summary = "查询积分和会员资源返佣配置")
    @PreAuthorize("hasAuthority('membership:commission:detail')")
    public R<CommissionConfigVO> commissionConfig() {
        return R.ok(membershipService.commissionConfigVO());
    }

    @PutMapping("/api/admin/membership/commission-config")
    @Operation(summary = "修改积分单价和会员首次领取固定返佣")
    @OperationLog(module = "会员管理", value = "修改资源返佣配置")
    @PreAuthorize("hasAuthority('membership:commission:update')")
    public R<Void> updateCommissionConfig(@Valid @RequestBody CommissionConfigRequest request) {
        membershipService.updateCommissionConfig(request);
        return R.ok();
    }
}
