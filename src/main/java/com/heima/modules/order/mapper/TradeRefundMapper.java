package com.heima.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.order.entity.OrderEntities.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TradeRefundMapper extends BaseMapper<Refund> {
    @Select("SELECT * FROM trade_refund WHERE id=#{id} FOR UPDATE")
    Refund selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM trade_refund WHERE status IN ('APPROVED','PROCESSING','ABNORMAL') ORDER BY id LIMIT #{limit}")
    List<Refund> selectPendingChannelSync(@Param("limit") int limit);
}
