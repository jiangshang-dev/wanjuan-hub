package com.heima.modules.points.service;

import com.heima.common.PageResult;
import com.heima.modules.points.dto.PointsDtos.AdjustmentRequest;
import com.heima.modules.points.dto.PointsDtos.LevelRequest;
import com.heima.modules.points.dto.PointsDtos.RuleRequest;
import com.heima.modules.points.dto.PointsDtos.StreakRuleRequest;
import com.heima.modules.points.entity.PointsEntities.Level;
import com.heima.modules.points.entity.PointsEntities.Rule;
import com.heima.modules.points.entity.PointsEntities.StreakRule;
import com.heima.modules.points.vo.PointsVOs.AccountVO;
import com.heima.modules.points.vo.PointsVOs.LevelVO;
import com.heima.modules.points.vo.PointsVOs.MembershipVO;
import com.heima.modules.points.vo.PointsVOs.RecordVO;

import java.util.List;

public interface PointsService {
    AccountVO signIn(Long userId);
    AccountVO account(Long userId);
    PageResult<RecordVO> records(Long userId, long page, long size);
    PageResult<RecordVO> adminRecords(long page, long size);
    MembershipVO membership(Long userId);
    List<LevelVO> levels();
    PageResult<Rule> rules(long page, long size);
    Long createRule(RuleRequest request);
    void updateRule(Long id, RuleRequest request);
    List<StreakRule> streakRules();
    Long createStreakRule(StreakRuleRequest request);
    void updateStreakRule(Long id, StreakRuleRequest request);
    void deleteStreakRule(Long id);
    List<Level> adminLevels();
    Long createLevel(LevelRequest request);
    void updateLevel(Long id, LevelRequest request);
    boolean adjust(AdjustmentRequest request, Long adminId);
    boolean rewardApproved(Long userId, String bizType, Long bizId, Long taskId);
    boolean reverse(Long userId, String bizType, Long bizId, String reason);

    /**
     * 原子扣减资源兑换积分。resourceId参与幂等，调用方应在同一事务内发放资源权益。
     */
    boolean redeem(Long userId, Long resourceId, Integer points);
}
