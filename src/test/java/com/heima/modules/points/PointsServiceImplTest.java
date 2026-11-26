package com.heima.modules.points;

import com.heima.modules.points.entity.PointsEntities.Account;
import com.heima.modules.points.entity.PointsEntities.Record;
import com.heima.modules.points.entity.PointsEntities.SignIn;
import com.heima.modules.points.entity.PointsEntities.StreakRule;
import com.heima.modules.points.event.PointsRewardedEvent;
import com.heima.modules.points.mapper.PointsMappers.AccountMapper;
import com.heima.modules.points.mapper.PointsMappers.LevelMapper;
import com.heima.modules.points.mapper.PointsMappers.RecordMapper;
import com.heima.modules.points.mapper.PointsMappers.RuleMapper;
import com.heima.modules.points.mapper.PointsMappers.SignInMapper;
import com.heima.modules.points.mapper.PointsMappers.StreakRuleMapper;
import com.heima.modules.points.service.impl.PointsServiceImpl;
import com.heima.modules.points.vo.PointsVOs.AccountVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {
    @Mock private AccountMapper accountMapper;
    @Mock private RecordMapper recordMapper;
    @Mock private RuleMapper ruleMapper;
    @Mock private SignInMapper signInMapper;
    @Mock private StreakRuleMapper streakRuleMapper;
    @Mock private LevelMapper levelMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private PointsServiceImpl pointsService;

    @Test
    void signInContinuesYesterdayStreakAndUsesConfiguredReward() {
        Account account = account();
        LocalDate today = LocalDate.now();
        SignIn yesterday = signIn(today.minusDays(1), 4, 20);
        SignIn todayRecord = signIn(today, 5, 30);
        StreakRule rule = streakRule(5, 30);
        when(accountMapper.selectForUpdate(1L)).thenReturn(account);
        when(accountMapper.selectById(1L)).thenReturn(account);
        when(signInMapper.selectOne(any())).thenReturn(null, yesterday, todayRecord);
        when(streakRuleMapper.selectOne(any())).thenReturn(rule);

        AccountVO result = pointsService.signIn(1L);

        ArgumentCaptor<SignIn> signInCaptor = ArgumentCaptor.forClass(SignIn.class);
        verify(signInMapper).insert(signInCaptor.capture());
        assertEquals(5, signInCaptor.getValue().getStreakDays());
        assertEquals(30, signInCaptor.getValue().getPoints());
        ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
        verify(recordMapper).insert(recordCaptor.capture());
        assertEquals(30, recordCaptor.getValue().getChangeAmount());
        assertEquals(5, result.getStreakDays());
        assertEquals(30, result.getTodayPoints());
        assertTrue(result.getSignedToday());
        verify(eventPublisher).publishEvent(any(PointsRewardedEvent.class));
    }

    @Test
    void signInRestartsAtFirstDayAfterInterruptedStreak() {
        Account account = account();
        LocalDate today = LocalDate.now();
        SignIn todayRecord = signIn(today, 1, 10);
        when(accountMapper.selectForUpdate(1L)).thenReturn(account);
        when(accountMapper.selectById(1L)).thenReturn(account);
        when(signInMapper.selectOne(any())).thenReturn(null, null, todayRecord);
        when(streakRuleMapper.selectOne(any())).thenReturn(streakRule(1, 10));

        pointsService.signIn(1L);

        ArgumentCaptor<SignIn> captor = ArgumentCaptor.forClass(SignIn.class);
        verify(signInMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getStreakDays());
        assertEquals(10, captor.getValue().getPoints());
    }

    @Test
    void repeatedSignInDoesNotRewardAgain() {
        Account account = account();
        SignIn existing = signIn(LocalDate.now(), 3, 15);
        when(accountMapper.selectForUpdate(1L)).thenReturn(account);
        when(accountMapper.selectById(1L)).thenReturn(account);
        when(signInMapper.selectOne(any())).thenReturn(existing);
        when(streakRuleMapper.selectOne(any())).thenReturn(streakRule(4, 20));

        AccountVO result = pointsService.signIn(1L);

        verify(signInMapper, never()).insert(any(SignIn.class));
        verify(recordMapper, never()).insert(any(Record.class));
        verify(eventPublisher, never()).publishEvent(any());
        assertEquals(3, result.getStreakDays());
        assertEquals(20, result.getNextRewardPoints());
    }

    private static Account account() {
        Account account = new Account();
        account.setUserId(1L);
        account.setBalance(100);
        account.setTotalPoints(100L);
        account.setExperience(0L);
        account.setVersion(0);
        return account;
    }

    private static SignIn signIn(LocalDate date, int streakDays, int points) {
        SignIn signIn = new SignIn();
        signIn.setUserId(1L);
        signIn.setSignDate(date);
        signIn.setStreakDays(streakDays);
        signIn.setPoints(points);
        return signIn;
    }

    private static StreakRule streakRule(int day, int points) {
        StreakRule rule = new StreakRule();
        rule.setStreakDay(day);
        rule.setPoints(points);
        rule.setExperience(points);
        rule.setStatus(1);
        return rule;
    }
}
