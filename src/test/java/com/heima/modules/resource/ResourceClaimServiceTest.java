package com.heima.modules.resource;

import com.heima.exception.ServiceException;
import com.heima.modules.membership.entity.MembershipEntities.CommissionConfig;
import com.heima.modules.membership.entity.MembershipEntities.DailyUsage;
import com.heima.modules.membership.entity.MembershipEntities.ResourceClaim;
import com.heima.modules.membership.entity.MembershipEntities.UserMembership;
import com.heima.modules.membership.mapper.MembershipMappers.DailyUsageMapper;
import com.heima.modules.membership.mapper.MembershipMappers.ResourceClaimMapper;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.membership.vo.MembershipVOs.StatusVO;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.resource.service.impl.ResourceClaimServiceImpl;
import com.heima.modules.resource.vo.ResourceClaimVO;
import com.heima.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceClaimServiceTest {
    private static final LocalDate BUSINESS_DAY = LocalDate.of(2026, 11, 26);
    @Mock private ResourceInfoMapper resourceMapper;
    @Mock private ResourceVersionMapper versionMapper;
    @Mock private ResourceEntitlementMapper entitlementMapper;
    @Mock private ResourceClaimMapper claimMapper;
    @Mock private DailyUsageMapper dailyUsageMapper;
    @Mock private MembershipService membershipService;
    @Mock private WalletService walletService;
    @Mock private Clock clock;
    @InjectMocks private ResourceClaimServiceImpl claimService;

    private ResourceInfo resource;
    private ResourceEntitlement granted;
    private UserMembership membership;
    private DailyUsage usage;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-25T16:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
        resource = new ResourceInfo();
        resource.setId(1L);
        resource.setAuthorId(7L);
        resource.setDeleted(0);
        resource.setOnline(1);
        resource.setInvalid(0);
        resource.setPublishedVersionNo(1);
        ResourceVersion version = new ResourceVersion();
        version.setResourceId(1L);
        version.setVersionNo(1);
        version.setAccessType(AccessType.MEMBER.name());
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(versionMapper.selectOne(any())).thenReturn(version);
        granted = new ResourceEntitlement();
        granted.setStatus("ACTIVE");
        granted.setSourceType("MEMBER");
        granted.setGrantedAt(LocalDateTime.of(2026, 11, 26, 0, 0));
        membership = new UserMembership();
        membership.setUserId(8L);
        membership.setDailyResourceLimit(2);
        membership.setExpireAt(LocalDateTime.of(2026, 12, 1, 0, 0));
        usage = new DailyUsage();
        usage.setUserId(8L);
        usage.setClaimDate(BUSINESS_DAY);
        usage.setUsedCount(0);
    }

    @Test
    void repeatClaimUsesNoExtraQuotaAndPaysCommissionOnlyOnce() {
        when(entitlementMapper.selectOne(any())).thenReturn(null, granted, granted);
        when(membershipService.lockActiveMembership(8L)).thenReturn(membership);
        when(dailyUsageMapper.selectForUpdate(8L, BUSINESS_DAY)).thenReturn(usage);
        when(dailyUsageMapper.consume(8L, BUSINESS_DAY, 2)).thenReturn(1);
        when(claimMapper.insert(any(ResourceClaim.class))).thenAnswer(invocation -> {
            ResourceClaim claim = invocation.getArgument(0);
            claim.setId(99L);
            claim.setCreateTime(LocalDateTime.of(2026, 11, 26, 0, 0));
            return 1;
        });
        CommissionConfig config = new CommissionConfig();
        config.setMemberFixedAmount(new BigDecimal("2.50"));
        when(membershipService.commissionConfig()).thenReturn(config);
        StatusVO status = new StatusVO();
        status.setUsedToday(1);
        status.setRemainingToday(1);
        when(membershipService.status(8L)).thenReturn(status);

        ResourceClaimVO first = claimService.claim(8L, 1L);
        ResourceClaimVO repeated = claimService.claim(8L, 1L);

        assertFalse(first.getDuplicated());
        assertTrue(repeated.getDuplicated());
        assertEquals(new BigDecimal("2.50"), first.getCommissionAmount());
        assertEquals(BigDecimal.ZERO, repeated.getCommissionAmount());
        verify(dailyUsageMapper).consume(8L, BUSINESS_DAY, 2);
        verify(walletService).credit(7L, "MEMBER_RESOURCE_COMMISSION", "99", new BigDecimal("2.50"),
                "会员资源首次有效领取返佣，资源ID：1");
        verify(membershipService).lockActiveMembership(8L);
    }

    @Test
    void authorClaimingOwnResourceNeverReceivesCommission() {
        resource.setAuthorId(8L);
        when(entitlementMapper.selectOne(any())).thenReturn(null, granted);
        when(membershipService.lockActiveMembership(8L)).thenReturn(membership);
        when(dailyUsageMapper.selectForUpdate(8L, BUSINESS_DAY)).thenReturn(usage);
        when(dailyUsageMapper.consume(8L, BUSINESS_DAY, 2)).thenReturn(1);

        ResourceClaimVO result = claimService.claim(8L, 1L);

        assertEquals(BigDecimal.ZERO, result.getCommissionAmount());
        verifyNoInteractions(walletService);
        verify(membershipService, never()).commissionConfig();
    }

    @Test
    void exhaustedDailyQuotaRejectsBeforeClaimAndCommission() {
        when(entitlementMapper.selectOne(any())).thenReturn(null);
        when(membershipService.lockActiveMembership(8L)).thenReturn(membership);
        when(dailyUsageMapper.selectForUpdate(8L, BUSINESS_DAY)).thenReturn(usage);
        when(dailyUsageMapper.consume(8L, BUSINESS_DAY, 2)).thenReturn(0);

        ServiceException error = assertThrows(ServiceException.class, () -> claimService.claim(8L, 1L));

        assertEquals(429, error.getCode());
        verify(claimMapper, never()).insert(any(ResourceClaim.class));
        verifyNoInteractions(walletService);
    }
}
