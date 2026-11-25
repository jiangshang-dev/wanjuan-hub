package com.heima.modules.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.resource.dto.ResourceEntitlementQueryDTO;
import com.heima.modules.resource.entity.ResourceEntitlement;
import com.heima.modules.resource.mapper.ResourceEntitlementMapper;
import com.heima.modules.resource.service.impl.ResourceEntitlementServiceImpl;
import com.heima.modules.resource.vo.ResourceEntitlementAdminVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceEntitlementAdminServiceTest {
    @Mock private ResourceEntitlementMapper entitlementMapper;
    @InjectMocks private ResourceEntitlementServiceImpl service;

    @BeforeAll
    static void initTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ResourceEntitlement.class);
    }

    @Test
    void adminPageAppliesStatusAndUserFilters() {
        ResourceEntitlement entitlement = new ResourceEntitlement();
        entitlement.setId(9L);
        entitlement.setUserId(2L);
        entitlement.setResourceId(3L);
        entitlement.setSourceType("PAID");
        entitlement.setSourceOrderNo("O1");
        entitlement.setAmount(new BigDecimal("12.34"));
        entitlement.setStatus("ACTIVE");
        Page<ResourceEntitlement> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(entitlement));
        when(entitlementMapper.selectPage(any(), any())).thenReturn(page);

        ResourceEntitlementQueryDTO query = new ResourceEntitlementQueryDTO();
        query.setUserId(2L);
        query.setStatus("ACTIVE");
        PageResult<ResourceEntitlementAdminVO> result = service.adminPage(query);

        ArgumentCaptor<LambdaQueryWrapper> wrapper = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(entitlementMapper).selectPage(any(), wrapper.capture());
        assertTrue(wrapper.getValue().getSqlSegment().contains("user_id"));
        assertTrue(wrapper.getValue().getSqlSegment().contains("status"));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue(2L));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue("ACTIVE"));
        assertEquals(9L, result.getRecords().getFirst().getId());
        assertEquals(2L, result.getRecords().getFirst().getUserId());
        assertEquals("O1", result.getRecords().getFirst().getSourceOrderNo());
    }

    @Test
    void adminDetailReturnsCompleteRecord() {
        ResourceEntitlement entitlement = new ResourceEntitlement();
        entitlement.setId(9L);
        entitlement.setUserId(2L);
        entitlement.setResourceId(3L);
        entitlement.setStatus("REVOKED");
        entitlement.setRevokeReason("REFUND:R1");
        when(entitlementMapper.selectById(9L)).thenReturn(entitlement);

        ResourceEntitlementAdminVO result = service.adminDetail(9L);

        assertEquals(9L, result.getId());
        assertEquals("REVOKED", result.getStatus());
        assertEquals("REFUND:R1", result.getRevokeReason());
    }

    @Test
    void missingAdminDetailReturnsNotFound() {
        when(entitlementMapper.selectById(99L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.adminDetail(99L));

        assertEquals(404, exception.getCode());
    }
}
