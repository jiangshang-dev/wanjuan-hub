package com.heima.modules.order.service;

import com.heima.common.PageResult;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.vo.OrderVO;

public interface OrderService {
    OrderVO create(OrderDtos.CreateRequest request, Long userId);

    PageResult<OrderVO> userPage(OrderDtos.Query query, Long userId);

    PageResult<OrderVO> adminPage(OrderDtos.Query query);

    OrderVO userDetail(String orderNo, Long userId);

    OrderVO adminDetail(String orderNo);

    OrderVO toVO(Order order, boolean includeDetails);
}
