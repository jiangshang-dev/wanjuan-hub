package com.heima.modules.resource;

import com.heima.modules.audit.service.AuditService;
import com.heima.modules.file.service.FileService;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.mapper.ResourceAccessRecordMapper;
import com.heima.modules.resource.mapper.ResourceDiskTypeMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceLinkMapper;
import com.heima.modules.resource.mapper.ResourceReplyMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.resource.service.ResourceCryptoService;
import com.heima.modules.resource.service.ResourceEntitlementService;
import com.heima.modules.resource.service.ResourceUrlValidator;
import com.heima.modules.resource.service.impl.ResourceAccessServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAccessServiceTest {
    @Mock private ResourceInfoMapper resourceMapper;
    @Mock private ResourceVersionMapper versionMapper;
    @Mock private ResourceLinkMapper linkMapper;
    @Mock private ResourceDiskTypeMapper diskTypeMapper;
    @Mock private ResourceAccessRecordMapper accessRecordMapper;
    @Mock private ResourceReplyMapper replyMapper;
    @Mock private ResourceEntitlementService entitlementService;
    @Mock private ResourceCryptoService cryptoService;
    @Mock private ResourceUrlValidator urlValidator;
    @Mock private AuditService auditService;
    @Mock private FileService fileService;
    @InjectMocks private ResourceAccessServiceImpl accessService;

    @Test
    void guestFreeAllowsAnonymousButLoginFreeRequiresLogin() {
        assertTrue(accessService.decide(resource(), version(AccessType.GUEST_FREE), null, false).granted());
        assertFalse(accessService.decide(resource(), version(AccessType.LOGIN_FREE), null, false).granted());
        assertTrue(accessService.decide(resource(), version(AccessType.LOGIN_FREE), 8L, false).granted());
    }

    @Test
    void paidAccessDistinguishesPurchasedAndUnpurchasedUsers() {
        ResourceInfo resource = resource();
        ResourceVersion paid = version(AccessType.PAID);
        when(entitlementService.hasActiveAccess(8L, 1L)).thenReturn(true);
        when(entitlementService.hasActiveAccess(9L, 1L)).thenReturn(false);

        assertTrue(accessService.decide(resource, paid, 8L, false).granted());
        assertFalse(accessService.decide(resource, paid, 9L, false).granted());
        assertFalse(accessService.decide(resource, paid, null, false).granted());
    }

    @Test
    void unifiedFreeAndMemberTypesUseFreeAccessOrClaimedEntitlement() {
        assertTrue(accessService.decide(resource(), version(AccessType.FREE), null, false).granted());
        ResourceVersion member = version(AccessType.MEMBER);
        when(entitlementService.hasActiveAccess(8L, 1L)).thenReturn(false, true);
        assertFalse(accessService.decide(resource(), member, 8L, false).granted());
        assertTrue(accessService.decide(resource(), member, 8L, false).granted());
    }

    @Test
    void pointsAccessRequiresExistingEntitlement() {
        ResourceVersion points = version(AccessType.POINTS);
        when(entitlementService.hasActiveAccess(8L, 1L)).thenReturn(false, true);

        assertFalse(accessService.decide(resource(), points, 8L, false).granted());
        assertTrue(accessService.decide(resource(), points, 8L, false).granted());
    }

    @Test
    void replyAccessRequiresPublishedReplyWhoseAuditPassed() {
        ResourceInfo resource = resource();
        ResourceVersion replyRequired = version(AccessType.REPLY_REQUIRED);
        when(replyMapper.selectPublishedPostIds(1L, 8L)).thenReturn(List.of(55L));
        when(auditService.isApproved("FORUM_POST", 55L)).thenReturn(false, true);

        assertFalse(accessService.decide(resource, replyRequired, 8L, false).granted());
        assertTrue(accessService.decide(resource, replyRequired, 8L, false).granted());
    }

    @Test
    void authorAndAdminBypassPaymentButOrdinaryUserDoesNot() {
        ResourceVersion paid = version(AccessType.PAID);
        assertTrue(accessService.decide(resource(), paid, 7L, false).granted());
        assertTrue(accessService.decide(resource(), paid, 99L, true).granted());
    }

    private ResourceInfo resource() {
        ResourceInfo resource = new ResourceInfo();
        resource.setId(1L);
        resource.setAuthorId(7L);
        return resource;
    }

    private ResourceVersion version(AccessType accessType) {
        ResourceVersion version = new ResourceVersion();
        version.setResourceId(1L);
        version.setVersionNo(1);
        version.setAccessType(accessType.name());
        return version;
    }
}
