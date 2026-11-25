package com.heima.modules.resource;

import com.heima.exception.ServiceException;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceVersion;
import com.heima.modules.resource.enums.AccessType;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourcePointAccountMapper;
import com.heima.modules.resource.mapper.ResourcePointTransactionMapper;
import com.heima.modules.resource.mapper.ResourceVersionMapper;
import com.heima.modules.resource.service.impl.ResourceTradeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceTradeServiceTest {
    @Mock private ResourceInfoMapper resourceMapper;
    @Mock private ResourceVersionMapper versionMapper;
    @Mock private ResourceEntitlementMapper entitlementMapper;
    @Mock private ResourcePointAccountMapper pointAccountMapper;
    @Mock private ResourcePointTransactionMapper pointTransactionMapper;
    @InjectMocks private ResourceTradeServiceImpl tradeService;

    @Test
    void repeatedOrderGrantIsIdempotentAndDoesNotWriteAgain() {
        ResourceInfo resource = resource();
        ResourceEntitlement existing = new ResourceEntitlement();
        existing.setUserId(8L);
        existing.setResourceId(1L);
        existing.setSourceType("PAID");
        existing.setSourceOrderNo("ORDER-1");
        existing.setStatus("ACTIVE");
        when(resourceMapper.selectById(1L)).thenReturn(resource);
        when(versionMapper.selectOne(any())).thenReturn(version());
        when(entitlementMapper.selectOne(any())).thenReturn(existing);

        tradeService.grantPaidAccess(8L, 1L, "ORDER-1");
        tradeService.grantPaidAccess(8L, 1L, "ORDER-1");

        verify(entitlementMapper, never()).grantIgnore(any(), any(), anyString(), anyString(), any(), any());
        verify(entitlementMapper, never()).update(isNull(), any());
        verify(pointAccountMapper, never()).deduct(any(), any());
    }

    @Test
    void purchasedResourceCannotBePurchasedAgain() {
        when(resourceMapper.selectOne(any())).thenReturn(resource());
        when(versionMapper.selectOne(any())).thenReturn(version());
        when(entitlementMapper.selectCount(any())).thenReturn(1L);

        ServiceException error = assertThrows(ServiceException.class, () -> tradeService.getPaidPurchase(1L, 8L));

        assertEquals(409, error.getCode());
    }

    private ResourceInfo resource() {
        ResourceInfo resource = new ResourceInfo();
        resource.setId(1L);
        resource.setAuthorId(7L);
        resource.setDeleted(0);
        resource.setOnline(1);
        resource.setInvalid(0);
        resource.setLatestVersionNo(1);
        resource.setPublishedVersionNo(1);
        return resource;
    }

    private ResourceVersion version() {
        ResourceVersion version = new ResourceVersion();
        version.setResourceId(1L);
        version.setVersionNo(1);
        version.setTitle("付费资源");
        version.setAccessType(AccessType.PAID.name());
        version.setPrice(new BigDecimal("12.00"));
        return version;
    }
}
