package com.heima.modules.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.payment.entity.TradePayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TradePaymentMapper extends BaseMapper<TradePayment> {
    @Select("SELECT * FROM trade_payment WHERE order_id=#{orderId} FOR UPDATE")
    TradePayment selectByOrderForUpdate(@Param("orderId") Long orderId);

    @Select("SELECT * FROM trade_payment WHERE channel=#{channel} AND channel_trade_no=#{channelTradeNo}")
    TradePayment selectByChannelTradeNo(@Param("channel") String channel, @Param("channelTradeNo") String channelTradeNo);

    @Select("SELECT * FROM trade_payment WHERE status IN ('CREATED','PAYING') AND (last_query_at IS NULL OR last_query_at<=#{before}) ORDER BY id LIMIT #{limit}")
    List<TradePayment> selectNeedQuery(@Param("before") LocalDateTime before, @Param("limit") int limit);

    @Select("SELECT * FROM trade_payment WHERE status='SUCCEEDED' AND (last_query_at IS NULL OR last_query_at<=#{before}) ORDER BY id LIMIT #{limit}")
    List<TradePayment> selectSucceededForAudit(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
