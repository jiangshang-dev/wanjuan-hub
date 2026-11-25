package com.heima.modules.membership.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.membership.dto.MembershipDtos.CommissionConfigRequest;
import com.heima.modules.membership.dto.MembershipDtos.PlanQuery;
import com.heima.modules.membership.dto.MembershipDtos.PlanRequest;
import com.heima.modules.membership.entity.MembershipEntities.CommissionConfig;
import com.heima.modules.membership.entity.MembershipEntities.DailyUsage;
import com.heima.modules.membership.entity.MembershipEntities.Plan;
import com.heima.modules.membership.entity.MembershipEntities.UserMembership;
import com.heima.modules.membership.mapper.MembershipMappers.CommissionConfigMapper;
import com.heima.modules.membership.mapper.MembershipMappers.DailyUsageMapper;
import com.heima.modules.membership.mapper.MembershipMappers.PlanMapper;
import com.heima.modules.membership.mapper.MembershipMappers.UserMembershipMapper;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.membership.vo.MembershipVOs.CommissionConfigVO;
import com.heima.modules.membership.vo.MembershipVOs.PlanVO;
import com.heima.modules.membership.vo.MembershipVOs.StatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {
    private final PlanMapper planMapper;
    private final UserMembershipMapper membershipMapper;
    private final DailyUsageMapper dailyUsageMapper;
    private final CommissionConfigMapper commissionConfigMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<PlanVO> publicPlans() {
        return planMapper.selectList(new LambdaQueryWrapper<Plan>().eq(Plan::getStatus, 1)
                        .orderByAsc(Plan::getSortOrder).orderByAsc(Plan::getId))
                .stream().map(this::planVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PlanVO> adminPlans(PlanQuery query) {
        Page<Plan> result = planMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Plan>().eq(query.getStatus() != null, Plan::getStatus, query.getStatus())
                        .orderByAsc(Plan::getSortOrder).orderByDesc(Plan::getId));
        return PageResult.of(result.getRecords().stream().map(this::planVO).toList(), result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    @Transactional
    public Long createPlan(PlanRequest request) {
        Plan plan = new Plan();
        apply(plan, request);
        planMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional
    public void updatePlan(Long id, PlanRequest request) {
        Plan plan = planMapper.selectById(id);
        if (plan == null) throw new ServiceException(404, "会员套餐不存在");
        apply(plan, request);
        planMapper.updateById(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Plan enabledPlan(Long id) {
        Plan plan = planMapper.selectById(id);
        if (plan == null || !Integer.valueOf(1).equals(plan.getStatus())) {
            throw new ServiceException(404, "会员套餐不存在或已停用");
        }
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public StatusVO status(Long userId) {
        UserMembership membership = membershipMapper.selectById(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);
        DailyUsage usage = dailyUsageMapper.selectOne(new LambdaQueryWrapper<DailyUsage>()
                .eq(DailyUsage::getUserId, userId).eq(DailyUsage::getClaimDate, today));
        boolean active = active(membership, now);
        int dailyLimit = active ? membership.getDailyResourceLimit() : 0;
        int used = usage == null ? 0 : usage.getUsedCount();
        StatusVO vo = new StatusVO();
        vo.setUserId(userId);
        vo.setActive(active);
        vo.setClaimDate(today);
        vo.setUsedToday(used);
        vo.setDailyResourceLimit(dailyLimit);
        vo.setRemainingToday(Math.max(0, dailyLimit - used));
        if (membership != null) {
            vo.setPlanId(membership.getPlanId());
            vo.setExpireAt(membership.getExpireAt());
            if (membership.getPlanId() != null) {
                Plan plan = planMapper.selectById(membership.getPlanId());
                if (plan != null) vo.setPlanName(plan.getName());
            }
        }
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long userId) {
        return userId != null && active(membershipMapper.selectById(userId), LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public UserMembership lockActiveMembership(Long userId) {
        UserMembership membership = membershipMapper.selectForUpdate(userId);
        if (!active(membership, LocalDateTime.now(clock))) throw new ServiceException(403, "会员已过期或尚未开通");
        return membership;
    }

    @Override
    @Transactional
    public void activate(Long userId, Long planId, Integer durationDays, Integer dailyLimit, String orderNo) {
        if (userId == null || planId == null || durationDays == null || durationDays <= 0
                || dailyLimit == null || dailyLimit <= 0 || orderNo == null || orderNo.isBlank()) {
            throw new ServiceException(500, "会员订单开通参数不完整");
        }
        membershipMapper.createIgnore(userId);
        UserMembership membership = membershipMapper.selectForUpdate(userId);
        if (membership == null) throw new ServiceException(500, "会员账户创建失败");
        if (orderNo.equals(membership.getLastOrderNo())) return;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime base = active(membership, now) ? membership.getExpireAt() : now;
        membership.setPlanId(planId);
        membership.setDailyResourceLimit(dailyLimit);
        membership.setExpireAt(base.plusDays(durationDays));
        membership.setLastOrderNo(orderNo);
        membership.setVersion(membership.getVersion() + 1);
        membershipMapper.updateById(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionConfig commissionConfig() {
        CommissionConfig config = commissionConfigMapper.selectById(1L);
        if (config == null) throw new ServiceException(500, "资源返佣配置不存在");
        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionConfigVO commissionConfigVO() {
        CommissionConfigVO vo = new CommissionConfigVO();
        BeanUtils.copyProperties(commissionConfig(), vo);
        return vo;
    }

    @Override
    @Transactional
    public void updateCommissionConfig(CommissionConfigRequest request) {
        CommissionConfig config = commissionConfig();
        config.setPointsUnitPrice(request.getPointsUnitPrice());
        config.setMemberFixedAmount(request.getMemberFixedAmount());
        commissionConfigMapper.updateById(config);
    }

    private void apply(Plan plan, PlanRequest request) {
        plan.setName(request.getName().trim());
        plan.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        plan.setDurationDays(request.getDurationDays());
        plan.setPrice(request.getPrice());
        plan.setDailyResourceLimit(request.getDailyResourceLimit());
        plan.setStatus(request.getStatus());
        plan.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private PlanVO planVO(Plan plan) {
        PlanVO vo = new PlanVO();
        BeanUtils.copyProperties(plan, vo);
        return vo;
    }

    private static boolean active(UserMembership membership, LocalDateTime now) {
        return membership != null && membership.getExpireAt() != null && membership.getExpireAt().isAfter(now);
    }
}
