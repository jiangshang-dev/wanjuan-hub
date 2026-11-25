package com.heima.modules.membership.service;

import com.heima.common.PageResult;
import com.heima.modules.membership.dto.MembershipDtos.CommissionConfigRequest;
import com.heima.modules.membership.dto.MembershipDtos.PlanQuery;
import com.heima.modules.membership.dto.MembershipDtos.PlanRequest;
import com.heima.modules.membership.entity.MembershipEntities.CommissionConfig;
import com.heima.modules.membership.entity.MembershipEntities.Plan;
import com.heima.modules.membership.entity.MembershipEntities.UserMembership;
import com.heima.modules.membership.vo.MembershipVOs.CommissionConfigVO;
import com.heima.modules.membership.vo.MembershipVOs.PlanVO;
import com.heima.modules.membership.vo.MembershipVOs.StatusVO;

import java.util.List;

public interface MembershipService {
    List<PlanVO> publicPlans();
    PageResult<PlanVO> adminPlans(PlanQuery query);
    Long createPlan(PlanRequest request);
    void updatePlan(Long id, PlanRequest request);
    Plan enabledPlan(Long id);
    StatusVO status(Long userId);
    boolean isActive(Long userId);
    UserMembership lockActiveMembership(Long userId);
    void activate(Long userId, Long planId, Integer durationDays, Integer dailyLimit, String orderNo);
    CommissionConfig commissionConfig();
    CommissionConfigVO commissionConfigVO();
    void updateCommissionConfig(CommissionConfigRequest request);
}
