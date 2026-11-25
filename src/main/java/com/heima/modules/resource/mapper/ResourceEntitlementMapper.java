package com.heima.modules.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.resource.entity.ResourceEntitlement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface ResourceEntitlementMapper extends BaseMapper<ResourceEntitlement> {
    @Insert("""
            INSERT INTO resource_entitlement(user_id, resource_id, source_type, source_order_no, amount, points_amount, status, granted_at)
            VALUES(#{userId}, #{resourceId}, #{sourceType}, #{orderNo}, #{amount}, #{pointsAmount}, 'ACTIVE', CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE id = id
            """)
    int grantIgnore(@Param("userId") Long userId, @Param("resourceId") Long resourceId,
                    @Param("sourceType") String sourceType, @Param("orderNo") String orderNo,
                    @Param("amount") BigDecimal amount, @Param("pointsAmount") Integer pointsAmount);

    @Update("""
            UPDATE resource_entitlement SET status = 'REVOKED', revoke_reason = #{reason}, revoked_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND resource_id = #{resourceId} AND source_type = 'PAID' AND status = 'ACTIVE'
            """)
    int revokePaid(@Param("userId") Long userId, @Param("resourceId") Long resourceId, @Param("reason") String reason);
}
