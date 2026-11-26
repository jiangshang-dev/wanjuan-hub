package com.heima.modules.membership;

import com.heima.modules.membership.entity.MembershipEntities.UserMembership;
import com.heima.modules.membership.mapper.MembershipMappers.CommissionConfigMapper;
import com.heima.modules.membership.mapper.MembershipMappers.DailyUsageMapper;
import com.heima.modules.membership.mapper.MembershipMappers.PlanMapper;
import com.heima.modules.membership.mapper.MembershipMappers.UserMembershipMapper;
import com.heima.modules.membership.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {
    @Mock private PlanMapper planMapper;
    @Mock private UserMembershipMapper membershipMapper;
    @Mock private DailyUsageMapper dailyUsageMapper;
    @Mock private CommissionConfigMapper commissionConfigMapper;
    @Mock private Clock clock;
    @InjectMocks private MembershipServiceImpl membershipService;

    private UserMembership membership;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-25T16:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
        membership = new UserMembership();
        membership.setUserId(8L);
        membership.setPlanId(1L);
        membership.setDailyResourceLimit(3);
        membership.setExpireAt(LocalDateTime.of(2026, 12, 1, 0, 0));
        membership.setVersion(0);
        when(membershipMapper.selectForUpdate(8L)).thenReturn(membership);
    }

    @Test
    void renewalAccumulatesRemainingDurationAndUsesLatestDailyLimit() {
        membershipService.activate(8L, 2L, 30, 5, "M1");
        assertEquals(LocalDateTime.of(2026, 12, 31, 0, 0), membership.getExpireAt());
        assertEquals(5, membership.getDailyResourceLimit());

        membershipService.activate(8L, 3L, 10, 8, "M2");

        assertEquals(LocalDateTime.of(2027, 1, 10, 0, 0), membership.getExpireAt());
        assertEquals(8, membership.getDailyResourceLimit());
        assertEquals("M2", membership.getLastOrderNo());
        verify(membershipMapper, times(2)).updateById(membership);
    }

    @Test
    void repeatedPaidOrderDoesNotExtendMembershipAgain() {
        membershipService.activate(8L, 2L, 30, 5, "M1");
        LocalDateTime firstExpire = membership.getExpireAt();

        membershipService.activate(8L, 2L, 30, 5, "M1");

        assertEquals(firstExpire, membership.getExpireAt());
        verify(membershipMapper, times(1)).updateById(membership);
    }
}
