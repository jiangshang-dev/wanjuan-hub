package com.heima.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.PaymentChannelGateway;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelRefund;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.RefundNotification;
import com.heima.infrastructure.payment.PaymentGatewayRegistry;
import com.heima.modules.order.dto.OrderDtos;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.entity.OrderEntities.Refund;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.order.mapper.TradeRefundMapper;
import com.heima.modules.order.service.OrderServiceImpl;
import com.heima.modules.order.vo.OrderVO;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.entity.TradePaymentNotification;
import com.heima.modules.payment.event.PaymentCompletedEvent;
import com.heima.modules.payment.event.PaymentEventPublisher;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.mapper.TradePaymentNotificationMapper;
import com.heima.modules.resource.service.ResourceTradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundProcessingService {
    private final TradeRefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final TradePaymentMapper paymentMapper;
    private final TradePaymentNotificationMapper notificationMapper;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final ResourceTradeService resourceTradeService;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public OrderVO.RefundVO request(String orderNo, OrderDtos.RefundRequest request, Long userId) {
        Order order = lockedOrder(orderNo);
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException(404, "订单不存在");
        }
        Refund existing = refundMapper.selectOne(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, order.getId()));
        if (existing != null) {
            return OrderServiceImpl.toRefundVO(existing, orderNo);
        }
        if ("MEMBERSHIP".equals(order.getOrderType())) {
            throw new ServiceException(409, "会员订单暂不支持自助退款，请联系管理员处理");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new ServiceException(409, "仅已支付订单可以申请退款");
        }
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        if (payment == null || !"SUCCEEDED".equals(payment.getStatus())) {
            throw new ServiceException(409, "支付单尚未成功，不能退款");
        }
        Refund refund = new Refund();
        refund.setRefundNo("R" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        refund.setOrderId(order.getId());
        refund.setPaymentId(payment.getId());
        refund.setUserId(userId);
        refund.setChannel(payment.getChannel());
        refund.setAmount(order.getTotalAmount());
        refund.setCurrency("CNY");
        refund.setReason(request.getReason().trim());
        refund.setStatus("PENDING_REVIEW");
        refund.setEntitlementRevoked(0);
        refund.setVersion(0);
        refundMapper.insert(refund);
        order.setStatus("REFUNDING");
        orderMapper.updateById(order);
        return OrderServiceImpl.toRefundVO(refund, orderNo);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO.RefundVO> userPage(OrderDtos.RefundQuery query, Long userId) {
        return page(query, new LambdaQueryWrapper<Refund>().eq(Refund::getUserId, userId)
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Refund::getStatus, query.getStatus()));
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO.RefundVO> adminPage(OrderDtos.RefundQuery query) {
        return page(query, new LambdaQueryWrapper<Refund>()
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Refund::getStatus, query.getStatus()));
    }

    @Transactional
    public OrderVO.RefundVO review(Long refundId, OrderDtos.RefundReviewRequest request, Long adminId) {
        Refund refund = refundMapper.selectForUpdate(refundId);
        if (refund == null) {
            throw new ServiceException(404, "退款单不存在");
        }
        Order order = lockedOrder(refund.getOrderId());
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        if (payment == null || !refund.getPaymentId().equals(payment.getId())) {
            throw new ServiceException(409, "退款支付单不匹配");
        }
        if (!"PENDING_REVIEW".equals(refund.getStatus())) {
            throw new ServiceException(409, "退款单已审核或正在渠道处理");
        }
        refund.setReviewerId(adminId);
        refund.setReviewOpinion(request.getOpinion());
        refund.setReviewedAt(LocalDateTime.now());
        if ("REJECT".equals(request.getAction())) {
            refund.setStatus("REJECTED");
            refundMapper.updateById(refund);
            order.setStatus("PAID");
            orderMapper.updateById(order);
            return OrderServiceImpl.toRefundVO(refund, order.getOrderNo());
        }

        refund.setStatus("APPROVED");
        refundMapper.updateById(refund);
        PaymentChannelGateway gateway = gatewayRegistry.get(refund.getChannel());
        ChannelRefund channelRefund = gateway.createRefund(new RefundCommand(refund.getRefundNo(), order.getOrderNo(),
                payment.getChannelTradeNo(), refund.getAmount(), order.getTotalAmount(), refund.getReason()));
        applyChannelResult(refund, order, payment, channelRefund);
        return OrderServiceImpl.toRefundVO(refund, order.getOrderNo());
    }

    @Transactional
    public OrderVO.RefundVO synchronize(String refundNo) {
        Refund refund = refundByNo(refundNo);
        refund = refundMapper.selectForUpdate(refund.getId());
        if ("SUCCEEDED".equals(refund.getStatus()) || "REJECTED".equals(refund.getStatus())) {
            return OrderServiceImpl.toRefundVO(refund, lockedOrder(refund.getOrderId()).getOrderNo());
        }
        Order order = lockedOrder(refund.getOrderId());
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        ChannelRefund result = gatewayRegistry.get(refund.getChannel()).queryRefund(refundNo, order.getOrderNo());
        applyChannelResult(refund, order, payment, result);
        return OrderServiceImpl.toRefundVO(refund, order.getOrderNo());
    }

    @Transactional
    public void handleNotification(String channel, RefundNotification notification) {
        TradePaymentNotification existing = notificationMapper.selectOne(new LambdaQueryWrapper<TradePaymentNotification>()
                .eq(TradePaymentNotification::getNotificationId, notification.notificationId()));
        if (existing != null) {
            return;
        }
        Refund refund = refundMapper.selectForUpdate(refundByNo(notification.refundNo()).getId());
        Order order = lockedOrder(refund.getOrderId());
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        if (!channel.equals(refund.getChannel()) || payment == null || !refund.getPaymentId().equals(payment.getId())) {
            throw new ServiceException(400, "退款通知渠道或支付单不匹配");
        }
        TradePaymentNotification record = PaymentProcessingService.notificationRecord(notification.notificationId(), channel,
                order, payment, notification.channelTradeNo(), notification.eventType(), notification.rawPayload());
        notificationMapper.insert(record);
        if (notification.success()) {
            applyChannelResult(refund, order, payment, new ChannelRefund(notification.refundNo(), notification.orderNo(),
                    notification.channelTradeNo(), notification.channelRefundNo(), notification.status(), notification.amount(),
                    notification.currency(), true));
        } else {
            refund.setChannelStatus(notification.status());
            if ("ABNORMAL".equals(notification.status()) || "CLOSED".equals(notification.status())) {
                refund.setStatus("ABNORMAL");
                refund.setErrorMessage("渠道退款未完成，请审核后重试或人工核查");
                refundMapper.updateById(refund);
            }
        }
        record.setProcessStatus(notification.success() ? "PROCESSED" : "IGNORED");
        record.setProcessedAt(LocalDateTime.now());
        notificationMapper.updateById(record);
    }

    private void applyChannelResult(Refund refund, Order order, TradePayment payment, ChannelRefund result) {
        if (!refund.getRefundNo().equals(result.refundNo()) || !order.getOrderNo().equals(result.orderNo())) {
            throw new ServiceException(502, "渠道退款单号或订单号不匹配");
        }
        if (result.channelTradeNo() != null && !result.channelTradeNo().equals(payment.getChannelTradeNo())) {
            throw new ServiceException(400, "渠道退款对应的支付交易号不匹配");
        }
        refund.setChannelStatus(result.status());
        if (result.channelRefundNo() != null) {
            refund.setChannelRefundNo(result.channelRefundNo());
        }
        if (result.succeeded()) {
            if (result.amount() == null || result.amount().compareTo(refund.getAmount()) != 0
                    || result.amount().compareTo(order.getTotalAmount()) != 0 || !"CNY".equals(result.currency())) {
                throw new ServiceException(400, "渠道退款金额或币种不匹配");
            }
            complete(refund, order, payment);
            return;
        }
        if ("ABNORMAL".equals(result.status()) || "CLOSED".equals(result.status())) {
            refund.setStatus("ABNORMAL");
            refund.setErrorMessage("渠道退款异常，请人工核查");
        } else {
            refund.setStatus("PROCESSING");
        }
        refundMapper.updateById(refund);
    }

    private void complete(Refund refund, Order order, TradePayment payment) {
        if ("SUCCEEDED".equals(refund.getStatus()) && Integer.valueOf(1).equals(refund.getEntitlementRevoked())) {
            return;
        }
        resourceTradeService.revokePaidAccess(order.getUserId(), order.getResourceId(), "REFUND:" + refund.getRefundNo());
        refund.setStatus("SUCCEEDED");
        refund.setRefundedAt(LocalDateTime.now());
        refund.setEntitlementRevoked(1);
        refund.setErrorMessage(null);
        refundMapper.updateById(refund);
        order.setStatus("REFUNDED");
        order.setGrantStatus("REVOKED");
        orderMapper.updateById(order);
        payment.setStatus("REFUNDED");
        paymentMapper.updateById(payment);
        eventPublisher.publish(new PaymentCompletedEvent(order.getOrderNo(), order.getUserId(), order.getResourceId(),
                refund.getAmount(), "REFUNDED"));
    }

    private PageResult<OrderVO.RefundVO> page(OrderDtos.RefundQuery query, LambdaQueryWrapper<Refund> wrapper) {
        Page<Refund> page = refundMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper.orderByDesc(Refund::getId));
        return PageResult.of(page.getRecords().stream().map(refund -> {
            Order order = orderMapper.selectById(refund.getOrderId());
            return OrderServiceImpl.toRefundVO(refund, order.getOrderNo());
        }).toList(), page.getTotal(), query.getPage(), query.getSize());
    }

    private Refund refundByNo(String refundNo) {
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>().eq(Refund::getRefundNo, refundNo));
        if (refund == null) {
            throw new ServiceException(404, "退款单不存在");
        }
        return refund;
    }

    private Order lockedOrder(String orderNo) {
        Order order = orderMapper.selectForUpdate(orderNo);
        if (order == null) {
            throw new ServiceException(404, "订单不存在");
        }
        return order;
    }

    private Order lockedOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException(404, "订单不存在");
        }
        return lockedOrder(order.getOrderNo());
    }
}
