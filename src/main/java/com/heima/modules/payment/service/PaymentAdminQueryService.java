package com.heima.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.payment.dto.PaymentAdminDtos;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.entity.TradePaymentNotification;
import com.heima.modules.payment.entity.TradeReconciliation;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.mapper.TradePaymentNotificationMapper;
import com.heima.modules.payment.mapper.TradeReconciliationMapper;
import com.heima.modules.payment.vo.PaymentAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentAdminQueryService {
    private final TradePaymentMapper paymentMapper;
    private final TradePaymentNotificationMapper notificationMapper;
    private final TradeReconciliationMapper reconciliationMapper;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public PageResult<PaymentAdminVO.PaymentVO> paymentPage(PaymentAdminDtos.PaymentQuery query) {
        Order order = filteredOrder(query.getOrderNo());
        if (hasText(query.getOrderNo()) && order == null) {
            return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
        }
        LambdaQueryWrapper<TradePayment> wrapper = new LambdaQueryWrapper<TradePayment>()
                .eq(hasText(query.getPaymentNo()), TradePayment::getPaymentNo, query.getPaymentNo())
                .eq(order != null, TradePayment::getOrderId, order == null ? null : order.getId())
                .eq(hasText(query.getChannel()), TradePayment::getChannel, query.getChannel())
                .eq(hasText(query.getStatus()), TradePayment::getStatus, query.getStatus())
                .orderByDesc(TradePayment::getId);
        Page<TradePayment> page = paymentMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, String> orderNumbers = orderNumbers(page.getRecords().stream().map(TradePayment::getOrderId).toList());
        return PageResult.of(page.getRecords().stream()
                .map(item -> paymentVO(item, orderNumbers.get(item.getOrderId()))).toList(),
                page.getTotal(), query.getPage(), query.getSize());
    }

    @Transactional(readOnly = true)
    public PaymentAdminVO.PaymentVO paymentDetail(String paymentNo) {
        TradePayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<TradePayment>()
                .eq(TradePayment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new ServiceException(404, "支付流水不存在");
        }
        return paymentVO(payment, orderNumber(payment.getOrderId()));
    }

    @Transactional(readOnly = true)
    public PageResult<PaymentAdminVO.NotificationVO> notificationPage(PaymentAdminDtos.NotificationQuery query) {
        Order order = filteredOrder(query.getOrderNo());
        if (hasText(query.getOrderNo()) && order == null) {
            return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
        }
        LambdaQueryWrapper<TradePaymentNotification> wrapper = new LambdaQueryWrapper<TradePaymentNotification>()
                .eq(hasText(query.getNotificationId()), TradePaymentNotification::getNotificationId, query.getNotificationId())
                .eq(order != null, TradePaymentNotification::getOrderId, order == null ? null : order.getId())
                .eq(hasText(query.getChannel()), TradePaymentNotification::getChannel, query.getChannel())
                .eq(hasText(query.getProcessStatus()), TradePaymentNotification::getProcessStatus, query.getProcessStatus())
                .orderByDesc(TradePaymentNotification::getId);
        Page<TradePaymentNotification> page = notificationMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, String> orderNumbers = orderNumbers(page.getRecords().stream().map(TradePaymentNotification::getOrderId).toList());
        return PageResult.of(page.getRecords().stream()
                .map(item -> notificationVO(item, orderNumbers.get(item.getOrderId()))).toList(),
                page.getTotal(), query.getPage(), query.getSize());
    }

    @Transactional(readOnly = true)
    public PaymentAdminVO.NotificationVO notificationDetail(String notificationId) {
        TradePaymentNotification notification = notificationMapper.selectOne(new LambdaQueryWrapper<TradePaymentNotification>()
                .eq(TradePaymentNotification::getNotificationId, notificationId));
        if (notification == null) {
            throw new ServiceException(404, "支付通知不存在");
        }
        return notificationVO(notification, orderNumber(notification.getOrderId()));
    }

    @Transactional(readOnly = true)
    public PageResult<PaymentAdminVO.ReconciliationVO> reconciliationPage(PaymentAdminDtos.ReconciliationQuery query) {
        Order order = filteredOrder(query.getOrderNo());
        if (hasText(query.getOrderNo()) && order == null) {
            return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
        }
        LambdaQueryWrapper<TradeReconciliation> wrapper = new LambdaQueryWrapper<TradeReconciliation>()
                .eq(hasText(query.getReconciliationNo()), TradeReconciliation::getReconciliationNo, query.getReconciliationNo())
                .eq(order != null, TradeReconciliation::getOrderId, order == null ? null : order.getId())
                .eq(hasText(query.getChannel()), TradeReconciliation::getChannel, query.getChannel())
                .eq(hasText(query.getResult()), TradeReconciliation::getResult, query.getResult())
                .orderByDesc(TradeReconciliation::getId);
        Page<TradeReconciliation> page = reconciliationMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, String> orderNumbers = orderNumbers(page.getRecords().stream().map(TradeReconciliation::getOrderId).toList());
        return PageResult.of(page.getRecords().stream()
                .map(item -> reconciliationVO(item, orderNumbers.get(item.getOrderId()))).toList(),
                page.getTotal(), query.getPage(), query.getSize());
    }

    @Transactional(readOnly = true)
    public PaymentAdminVO.ReconciliationVO reconciliationDetail(String reconciliationNo) {
        TradeReconciliation reconciliation = reconciliationMapper.selectOne(new LambdaQueryWrapper<TradeReconciliation>()
                .eq(TradeReconciliation::getReconciliationNo, reconciliationNo));
        if (reconciliation == null) {
            throw new ServiceException(404, "对账记录不存在");
        }
        return reconciliationVO(reconciliation, orderNumber(reconciliation.getOrderId()));
    }

    private Order filteredOrder(String orderNo) {
        if (!hasText(orderNo)) {
            return null;
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    private String orderNumber(Long orderId) {
        if (orderId == null) {
            return null;
        }
        Order order = orderMapper.selectById(orderId);
        return order == null ? null : order.getOrderNo();
    }

    private Map<Long, String> orderNumbers(Collection<Long> orderIds) {
        Set<Long> ids = orderIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Order::getId, Order::getOrderNo));
    }

    private static PaymentAdminVO.PaymentVO paymentVO(TradePayment payment, String orderNo) {
        PaymentAdminVO.PaymentVO vo = new PaymentAdminVO.PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        vo.setOrderNo(orderNo);
        return vo;
    }

    private static PaymentAdminVO.NotificationVO notificationVO(TradePaymentNotification notification, String orderNo) {
        PaymentAdminVO.NotificationVO vo = new PaymentAdminVO.NotificationVO();
        vo.setId(notification.getId());
        vo.setNotificationId(notification.getNotificationId());
        vo.setChannel(notification.getChannel());
        vo.setPaymentId(notification.getPaymentId());
        vo.setOrderId(notification.getOrderId());
        vo.setOrderNo(orderNo);
        vo.setChannelTradeNo(notification.getChannelTradeNo());
        vo.setEventType(notification.getEventType());
        vo.setProcessStatus(notification.getProcessStatus());
        vo.setErrorMessage(notification.getErrorMessage());
        vo.setProcessedAt(notification.getProcessedAt());
        vo.setCreateTime(notification.getCreateTime());
        vo.setUpdateTime(notification.getUpdateTime());
        return vo;
    }

    private static PaymentAdminVO.ReconciliationVO reconciliationVO(TradeReconciliation reconciliation, String orderNo) {
        PaymentAdminVO.ReconciliationVO vo = new PaymentAdminVO.ReconciliationVO();
        BeanUtils.copyProperties(reconciliation, vo);
        vo.setOrderNo(orderNo);
        return vo;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
