package com.heima.modules.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.points.dto.PointsDtos.AdjustmentRequest;
import com.heima.modules.points.dto.PointsDtos.LevelRequest;
import com.heima.modules.points.dto.PointsDtos.RuleRequest;
import com.heima.modules.points.dto.PointsDtos.StreakRuleRequest;
import com.heima.modules.points.entity.PointsEntities.Account;
import com.heima.modules.points.entity.PointsEntities.Level;
import com.heima.modules.points.entity.PointsEntities.Record;
import com.heima.modules.points.entity.PointsEntities.Rule;
import com.heima.modules.points.entity.PointsEntities.SignIn;
import com.heima.modules.points.entity.PointsEntities.StreakRule;
import com.heima.modules.points.event.PointsRewardedEvent;
import com.heima.modules.points.mapper.PointsMappers.AccountMapper;
import com.heima.modules.points.mapper.PointsMappers.LevelMapper;
import com.heima.modules.points.mapper.PointsMappers.RecordMapper;
import com.heima.modules.points.mapper.PointsMappers.RuleMapper;
import com.heima.modules.points.mapper.PointsMappers.SignInMapper;
import com.heima.modules.points.mapper.PointsMappers.StreakRuleMapper;
import com.heima.modules.points.service.PointsService;
import com.heima.modules.points.vo.PointsVOs.AccountVO;
import com.heima.modules.points.vo.PointsVOs.LevelVO;
import com.heima.modules.points.vo.PointsVOs.MembershipVO;
import com.heima.modules.points.vo.PointsVOs.RecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {
    private final AccountMapper accountMapper;
    private final RecordMapper recordMapper;
    private final RuleMapper ruleMapper;
    private final SignInMapper signInMapper;
    private final StreakRuleMapper streakRuleMapper;
    private final LevelMapper levelMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AccountVO signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String key = "SIGN_IN:" + userId + ":" + today;
        Account locked = lockAccount(userId);
        SignIn existing = signInMapper.selectOne(new LambdaQueryWrapper<SignIn>()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, today));
        if (existing == null) {
            SignIn yesterday = signInOnDate(userId, today.minusDays(1));
            int streakDays = yesterday == null ? 1 : defaultStreak(yesterday) + 1;
            StreakRule rule = streakReward(streakDays);
            int awarded = applyChange(locked, "SIGN_IN", null, key, rule.getPoints(),
                    rule.getExperience(), "连续签到第" + streakDays + "天", 0, false);
            SignIn signIn = new SignIn();
            signIn.setUserId(userId);
            signIn.setSignDate(today);
            signIn.setStreakDays(streakDays);
            signIn.setPoints(Math.max(awarded, 0));
            signIn.setIdempotencyKey(key);
            try {
                signInMapper.insert(signIn);
            } catch (DuplicateKeyException ignored) {
                // 用户与日期唯一键兜底，账户行锁下正常不会进入该分支。
            }
        }
        return account(userId);
    }

    @Override
    @Transactional
    public AccountVO account(Long userId) {
        Account account = accountMapper.selectById(userId);
        if (account == null) {
            accountMapper.insertIgnore(userId);
            account = accountMapper.selectById(userId);
        }
        LocalDate today = LocalDate.now();
        SignIn signedToday = signInOnDate(userId, today);
        SignIn yesterday = signedToday == null ? signInOnDate(userId, today.minusDays(1)) : null;
        int streakDays = signedToday != null ? defaultStreak(signedToday)
                : yesterday != null ? defaultStreak(yesterday) : 0;
        StreakRule next = streakReward(streakDays + 1);
        return new AccountVO(userId, account.getBalance(), account.getTotalPoints(), account.getExperience(),
                signedToday != null, streakDays, signedToday == null ? 0 : signedToday.getPoints(),
                streakDays + 1, next.getPoints());
    }

    @Override
    public PageResult<RecordVO> records(Long userId, long page, long size) {
        Page<Record> result = recordMapper.selectPage(new Page<>(page(page), size(size)),
                new LambdaQueryWrapper<Record>().eq(Record::getUserId, userId)
                        .orderByDesc(Record::getId));
        return PageResult.of(result.getRecords().stream().map(this::recordVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<RecordVO> adminRecords(long page, long size) {
        Page<Record> result = recordMapper.selectPage(new Page<>(page(page), size(size)),
                new LambdaQueryWrapper<Record>().orderByDesc(Record::getId));
        return PageResult.of(result.getRecords().stream().map(this::recordVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public MembershipVO membership(Long userId) {
        AccountVO account = account(userId);
        Level current = levelMapper.selectOne(new LambdaQueryWrapper<Level>()
                .eq(Level::getStatus, 1).le(Level::getMinExperience, account.getExperience())
                .orderByDesc(Level::getMinExperience).last("LIMIT 1"));
        Level next = levelMapper.selectOne(new LambdaQueryWrapper<Level>()
                .eq(Level::getStatus, 1).gt(Level::getMinExperience, account.getExperience())
                .orderByAsc(Level::getMinExperience).last("LIMIT 1"));
        long remaining = next == null ? 0 : next.getMinExperience() - account.getExperience();
        return new MembershipVO(account.getExperience(), levelVO(current), levelVO(next), remaining);
    }

    @Override
    public List<LevelVO> levels() {
        return levelMapper.selectList(new LambdaQueryWrapper<Level>().eq(Level::getStatus, 1)
                .orderByAsc(Level::getSortOrder).orderByAsc(Level::getLevel)).stream().map(this::levelVO).toList();
    }

    @Override
    public PageResult<Rule> rules(long page, long size) {
        Page<Rule> result = ruleMapper.selectPage(new Page<>(page(page), size(size)),
                new LambdaQueryWrapper<Rule>().orderByAsc(Rule::getId));
        return PageResult.of(result);
    }

    @Override
    @Transactional
    public Long createRule(RuleRequest request) {
        Rule rule = new Rule();
        BeanUtils.copyProperties(request, rule);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional
    public void updateRule(Long id, RuleRequest request) {
        Rule rule = requireRule(id);
        BeanUtils.copyProperties(request, rule);
        ruleMapper.updateById(rule);
    }

    @Override
    public List<StreakRule> streakRules() {
        return streakRuleMapper.selectList(new LambdaQueryWrapper<StreakRule>()
                .orderByAsc(StreakRule::getStreakDay));
    }

    @Override
    @Transactional
    public Long createStreakRule(StreakRuleRequest request) {
        StreakRule rule = new StreakRule();
        BeanUtils.copyProperties(request, rule);
        try {
            streakRuleMapper.insert(rule);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "该连续签到天数已配置");
        }
        return rule.getId();
    }

    @Override
    @Transactional
    public void updateStreakRule(Long id, StreakRuleRequest request) {
        StreakRule rule = streakRuleMapper.selectById(id);
        if (rule == null) throw new ServiceException(404, "连续签到档位不存在");
        BeanUtils.copyProperties(request, rule);
        try {
            streakRuleMapper.updateById(rule);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "该连续签到天数已配置");
        }
    }

    @Override
    @Transactional
    public void deleteStreakRule(Long id) {
        if (streakRuleMapper.deleteById(id) == 0) throw new ServiceException(404, "连续签到档位不存在");
    }

    @Override
    public List<Level> adminLevels() {
        return levelMapper.selectList(new LambdaQueryWrapper<Level>()
                .orderByAsc(Level::getSortOrder).orderByAsc(Level::getLevel));
    }

    @Override
    @Transactional
    public Long createLevel(LevelRequest request) {
        Level level = new Level();
        BeanUtils.copyProperties(request, level);
        levelMapper.insert(level);
        return level.getId();
    }

    @Override
    @Transactional
    public void updateLevel(Long id, LevelRequest request) {
        Level level = levelMapper.selectById(id);
        if (level == null) throw new ServiceException(404, "等级不存在");
        BeanUtils.copyProperties(request, level);
        levelMapper.updateById(level);
    }

    @Override
    @Transactional
    public boolean adjust(AdjustmentRequest request, Long adminId) {
        Account account = lockAccount(request.getUserId());
        String key = "ADMIN_ADJUST:" + adminId + ":" + request.getIdempotencyKey();
        applyChange(account, "ADMIN_ADJUST", adminId, key, request.getPoints(),
                request.getExperience(), request.getReason(), 0, false);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean rewardApproved(Long userId, String bizType, Long bizId, Long taskId) {
        if (userId == null) return false;
        Rule rule = ruleMapper.selectOne(new LambdaQueryWrapper<Rule>()
                .eq(Rule::getCode, bizType).eq(Rule::getStatus, 1).last("LIMIT 1"));
        if (rule == null || rule.getPoints() <= 0) return false;
        Account account = lockAccount(userId);
        String key = "AUDIT_REWARD:" + taskId;
        return applyChange(account, bizType, bizId, key, rule.getPoints(), rule.getExperience(),
                rule.getName(), rule.getDailyLimit(), false) > 0;
    }

    @Override
    @Transactional
    public boolean reverse(Long userId, String bizType, Long bizId, String reason) {
        List<Record> originals = recordMapper.selectList(new LambdaQueryWrapper<Record>()
                .eq(Record::getUserId, userId).eq(Record::getBizType, bizType)
                .eq(Record::getBizId, bizId).gt(Record::getChangeAmount, 0));
        boolean changed = false;
        for (Record original : originals) {
            Account account = lockAccount(userId);
            int result = applyChange(account, "VIOLATION_REVERSAL", original.getId(),
                    "REVERSAL:" + original.getId(), -original.getChangeAmount(),
                    -original.getExperienceChange(), reason, 0, true);
            changed |= result != 0;
        }
        return changed;
    }

    @Override
    @Transactional
    public boolean redeem(Long userId, Long resourceId, Integer points) {
        if (userId == null || resourceId == null || points == null || points <= 0) {
            throw new ServiceException("兑换积分必须大于0");
        }
        Account account = lockAccount(userId);
        String key = "RESOURCE_REDEEM:" + userId + ":" + resourceId;
        if (recordMapper.selectOne(new LambdaQueryWrapper<Record>()
                .eq(Record::getIdempotencyKey, key).last("LIMIT 1")) != null) return true;
        int result = applyChange(account, "RESOURCE_REDEEM", resourceId, key,
                -points, 0, "资源积分兑换", 0, false);
        return result != 0;
    }

    private int applyChange(Account account, String bizType, Long bizId, String key, int points,
                            int experience, String remark, int dailyLimit, boolean clampToAvailable) {
        Record duplicated = recordMapper.selectOne(new LambdaQueryWrapper<Record>()
                .eq(Record::getIdempotencyKey, key).last("LIMIT 1"));
        if (duplicated != null) return 0;

        if (points > 0 && dailyLimit > 0) {
            LocalDate today = LocalDate.now();
            int earned = recordMapper.sumDailyEarned(account.getUserId(), bizType,
                    today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            points = Math.min(points, Math.max(0, dailyLimit - earned));
            if (points == 0) return 0;
        }
        if (clampToAvailable) {
            points = -Math.min(-points, account.getBalance());
            experience = -Math.min(-experience, account.getExperience().intValue());
            if (points == 0 && experience == 0) return 0;
        }
        long balanceAfter = (long) account.getBalance() + points;
        long experienceAfter = account.getExperience() + experience;
        if (balanceAfter < 0) throw new ServiceException(409, "积分余额不足");
        if (experienceAfter < 0 || experienceAfter > Long.MAX_VALUE) {
            throw new ServiceException(409, "经验值不足或超出范围");
        }

        Record record = new Record();
        record.setUserId(account.getUserId());
        record.setChangeAmount(points);
        record.setBalanceAfter((int) balanceAfter);
        record.setExperienceChange(experience);
        record.setExperienceAfter(experienceAfter);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setIdempotencyKey(key);
        record.setRemark(remark == null ? "" : remark);
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            return 0;
        }

        if (points < 0 && accountMapper.deduct(account.getUserId(), -points) == 0) {
            throw new ServiceException(409, "积分余额不足");
        }
        account.setBalance((int) balanceAfter);
        account.setExperience(experienceAfter);
        account.setTotalPoints(account.getTotalPoints() + Math.max(points, 0));
        account.setVersion(account.getVersion() + 1);
        accountMapper.updateById(account);
        if (points > 0) {
            eventPublisher.publishEvent(new PointsRewardedEvent(account.getUserId(), bizType,
                    bizId, points, key));
        }
        return points;
    }

    private Account lockAccount(Long userId) {
        accountMapper.insertIgnore(userId);
        Account account = accountMapper.selectForUpdate(userId);
        if (account == null) throw new ServiceException(404, "用户积分账户不存在");
        return account;
    }

    private SignIn signInOnDate(Long userId, LocalDate date) {
        return signInMapper.selectOne(new LambdaQueryWrapper<SignIn>()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, date).last("LIMIT 1"));
    }

    private int defaultStreak(SignIn signIn) {
        return signIn.getStreakDays() == null ? 1 : signIn.getStreakDays();
    }

    private StreakRule streakReward(int streakDays) {
        StreakRule configured = streakRuleMapper.selectOne(new LambdaQueryWrapper<StreakRule>()
                .eq(StreakRule::getStatus, 1).le(StreakRule::getStreakDay, streakDays)
                .orderByDesc(StreakRule::getStreakDay).last("LIMIT 1"));
        if (configured != null) return configured;
        Rule base = enabledRule("SIGN_IN");
        StreakRule fallback = new StreakRule();
        fallback.setStreakDay(1);
        fallback.setPoints(base.getPoints());
        fallback.setExperience(base.getExperience());
        return fallback;
    }

    private Rule enabledRule(String code) {
        Rule rule = ruleMapper.selectOne(new LambdaQueryWrapper<Rule>()
                .eq(Rule::getCode, code).eq(Rule::getStatus, 1).last("LIMIT 1"));
        if (rule == null) throw new ServiceException(503, "签到规则未配置");
        return rule;
    }

    private Rule requireRule(Long id) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) throw new ServiceException(404, "积分规则不存在");
        return rule;
    }

    private RecordVO recordVO(Record record) {
        return new RecordVO(record.getId(), record.getChangeAmount(), record.getBalanceAfter(),
                record.getExperienceChange(), record.getExperienceAfter(), record.getBizType(),
                record.getBizId(), record.getRemark(), record.getCreateTime());
    }

    private LevelVO levelVO(Level level) {
        return level == null ? null : new LevelVO(level.getLevel(), level.getName(),
                level.getMinExperience(), level.getIcon(), level.getBenefits());
    }

    private static long page(long page) { return Math.max(1, page); }
    private static long size(long size) { return Math.min(100, Math.max(1, size)); }
}
