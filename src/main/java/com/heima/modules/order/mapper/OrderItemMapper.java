package com.heima.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.order.entity.OrderEntities.Item;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<Item> {
}
