package com.heima.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.order.entity.OrderEntities.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT * FROM trade_order WHERE order_no=#{orderNo} FOR UPDATE")
    Order selectForUpdate(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM trade_order WHERE status='PENDING_PAYMENT' AND expire_at<=#{now} ORDER BY id LIMIT #{limit}")
    List<Order> selectExpired(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("SELECT * FROM trade_order WHERE grant_status='PENDING' AND status IN ('PAID','REFUNDING') ORDER BY id LIMIT #{limit}")
    List<Order> selectPendingGrants(@Param("limit") int limit);
}
