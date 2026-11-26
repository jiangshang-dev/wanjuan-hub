package com.heima.modules.membership.controller;

import com.heima.common.R;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.membership.vo.MembershipVOs.PlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/membership/plans")
@RequiredArgsConstructor
@Tag(name = "公开会员套餐")
public class PublicMembershipController {
    private final MembershipService membershipService;

    @GetMapping
    @Operation(summary = "查询已启用的付费会员套餐")
    public R<List<PlanVO>> plans() {
        return R.ok(membershipService.publicPlans());
    }
}
