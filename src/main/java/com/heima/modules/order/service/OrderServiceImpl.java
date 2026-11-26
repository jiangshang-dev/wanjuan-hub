package com.heima.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.membership.dto.MembershipDtos.CreateOrderRequest;
import com.heima.modules.membership.entity.MembershipEntities.Plan;
import com.heima.modules.membership.service.MembershipService;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.entity.OrderEntities.Item;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.entity.OrderEntities.Refund;
import com.heima.modules.order.mapper.OrderItemMapper;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.order.mapper.TradeRefundMapper;
import com.heima.modules.order.vo.OrderVO;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.resource.service.ResourceTradeService;
import com.heima.modules.resource.service.ResourceTradeService.PurchaseItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final int PAYMENT_EXPIRE_MINUTES = 15;

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final TradePaymentMapper paymentMapper;
    private final TradeRefundMapper refundMapper;
    private final ResourceTradeService resourceTradeService;
    private final MembershipService membershipService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public OrderVO create(OrderDtos.CreateRequest request, Long userId) {
        String idempotencyKey = userId + ":" + request.getIdempotencyKey().trim();
        Order existing = byIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return repeatedOrder(existing, request.getResourceId(), userId);
        }
        try {
            Order created = transactionTemplate.execute(status -> {
                Order current = byIdempotencyKey(idempotencyKey);
                if (current != null) {
                    return current;
                }
                PurchaseItem purchase = resourceTradeService.getPaidPurchase(request.getResourceId(), userId);
                if (purchase == null || purchase.price() == null || purchase.price().signum() <= 0) {
                    throw new ServiceException(409, "资源当前不可购买");
                }
                if (userId.equals(purchase.ownerId())) {
                    throw new ServiceException(409, "不能购买自己的资源");
                }
                BigDecimal price = purchase.price().setScale(2, RoundingMode.UNNECESSARY);
                Order order = new Order();
                order.setOrderNo("O" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
                order.setIdempotencyKey(idempotencyKey);
                order.setUserId(userId);
                order.setOrderType("RESOURCE");
                order.setResourceId(purchase.resourceId());
                order.setTotalAmount(price);
                order.setCurrency("CNY");
                order.setClientType(request.getClientType());
                order.setStatus("PENDING_PAYMENT");
                order.setGrantStatus("NONE");
                order.setExpireAt(LocalDateTime.now().plusMinutes(PAYMENT_EXPIRE_MINUTES));
                order.setVersion(0);
                orderMapper.insert(order);

                Item item = new Item();
                item.setOrderId(order.getId());
                item.setResourceId(purchase.resourceId());
                item.setResourceName(purchase.name());
                item.setPrice(price);
                item.setOwnerId(purchase.ownerId());
                itemMapper.insert(item);
                return order;
            });
            if (created == null) {
                throw new ServiceException(500, "订单创建失败");
            }
            return toVO(created, true);
        } catch (DuplicateKeyException e) {
            Order repeated = byIdempotencyKey(idempotencyKey);
            if (repeated == null) {
                throw e;
            }
            return repeatedOrder(repeated, request.getResourceId(), userId);
        }
    }

    @Override
    public OrderVO createMembership(CreateOrderRequest request, Long userId) {
        String idempotencyKey = userId + ":MEMBERSHIP:" + request.getIdempotencyKey().trim();
        Order existing = byIdempotencyKey(idempotencyKey);
        if (existing != null) return repeatedMembershipOrder(existing, request.getPlanId(), userId);
        try {
            Order created = transactionTemplate.execute(status -> {
                Order current = byIdempotencyKey(idempotencyKey);
                if (current != null) return current;
                Plan plan = membershipService.enabledPlan(request.getPlanId());
                Order order = new Order();
                order.setOrderNo("M" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
                order.setIdempotencyKey(idempotencyKey);
                order.setUserId(userId);
                order.setOrderType("MEMBERSHIP");
                order.setMembershipPlanId(plan.getId());
                order.setMembershipDurationDays(plan.getDurationDays());
                order.setMembershipDailyLimit(plan.getDailyResourceLimit());
                order.setTotalAmount(plan.getPrice().setScale(2, RoundingMode.UNNECESSARY));
                order.setCurrency("CNY");
                order.setClientType(request.getClientType());
                order.setStatus("PENDING_PAYMENT");
                order.setGrantStatus("NONE");
                order.setExpireAt(LocalDateTime.now().plusMinutes(PAYMENT_EXPIRE_MINUTES));
                order.setVersion(0);
                orderMapper.insert(order);

                Item item = new Item();
                item.setOrderId(order.getId());
                item.setResourceName(plan.getName());
                item.setPrice(order.getTotalAmount());
                itemMapper.insert(item);
                return order;
            });
            if (created == null) throw new ServiceException(500, "会员订单创建失败");
            return toVO(created, true);
        } catch (DuplicateKeyException e) {
            Order repeated = byIdempotencyKey(idempotencyKey);
            if (repeated == null) throw e;
            return repeatedMembershipOrder(repeated, request.getPlanId(), userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderVO> userPage(OrderDtos.Query query, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId)
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Order::getStatus, query.getStatus())
                .eq(query.getOrderNo() != null && !query.getOrderNo().isBlank(), Order::getOrderNo, query.getOrderNo())
                .orderByDesc(Order::getId);
        return page(query, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderVO> adminPage(OrderDtos.Query query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Order::getStatus, query.getStatus())
                .eq(query.getOrderNo() != null && !query.getOrderNo().isBlank(), Order::getOrderNo, query.getOrderNo())
                .eq(query.getUserId() != null, Order::getUserId, query.getUserId())
                .orderByDesc(Order::getId);
        return page(query, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderVO userDetail(String orderNo, Long userId) {
        Order order = byOrderNo(orderNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException(404, "订单不存在");
        }
        return toVO(order, true);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderVO adminDetail(String orderNo) {
        Order order = byOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException(404, "订单不存在");
        }
        return toVO(order, true);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderVO toVO(Order order, boolean includeDetails) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        if (!includeDetails) {
            return vo;
        }
        List<OrderVO.ItemVO> items = itemMapper.selectList(new LambdaQueryWrapper<Item>().eq(Item::getOrderId, order.getId()))
                .stream().map(item -> {
                    OrderVO.ItemVO itemVO = new OrderVO.ItemVO();
                    BeanUtils.copyProperties(item, itemVO);
                    return itemVO;
                }).toList();
        vo.setItems(items);
        TradePayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<TradePayment>().eq(TradePayment::getOrderId, order.getId()));
        if (payment != null) {
            OrderVO.PaymentVO paymentVO = new OrderVO.PaymentVO();
            BeanUtils.copyProperties(payment, paymentVO);
            vo.setPayment(paymentVO);
        }
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, order.getId()));
        if (refund != null) {
            vo.setRefund(toRefundVO(refund, order.getOrderNo()));
        }
        return vo;
    }

    public static OrderVO.RefundVO toRefundVO(Refund refund, String orderNo) {
        OrderVO.RefundVO vo = new OrderVO.RefundVO();
        BeanUtils.copyProperties(refund, vo, "entitlementRevoked");
        vo.setOrderNo(orderNo);
        vo.setEntitlementRevoked(Integer.valueOf(1).equals(refund.getEntitlementRevoked()));
        return vo;
    }

    private PageResult<OrderVO> page(OrderDtos.Query query, LambdaQueryWrapper<Order> wrapper) {
        Page<Order> page = orderMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(order -> toVO(order, false)).toList(),
                page.getTotal(), query.getPage(), query.getSize());
    }

    private OrderVO repeatedOrder(Order order, Long resourceId, Long userId) {
        if (!userId.equals(order.getUserId()) || !resourceId.equals(order.getResourceId())) {
            throw new ServiceException(409, "幂等键已用于其他订单");
        }
        return toVO(order, true);
    }

    private OrderVO repeatedMembershipOrder(Order order, Long planId, Long userId) {
        if (!userId.equals(order.getUserId()) || !"MEMBERSHIP".equals(order.getOrderType())
                || !planId.equals(order.getMembershipPlanId())) {
            throw new ServiceException(409, "幂等键已用于其他订单");
        }
        return toVO(order, true);
    }

    private Order byIdempotencyKey(String key) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getIdempotencyKey, key));
    }

    private Order byOrderNo(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }
}
