package com.heima.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.payment.PaymentChannelGateway;
import com.heima.infrastructure.payment.PaymentGatewayModels.ChannelPayment;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentCommand;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentInstruction;
import com.heima.infrastructure.payment.PaymentGatewayModels.PaymentNotification;
import com.heima.infrastructure.payment.PaymentGatewayRegistry;
import com.heima.modules.order.entity.OrderEntities.Item;
import com.heima.modules.order.entity.OrderEntities.Order;
import com.heima.modules.order.mapper.OrderItemMapper;
import com.heima.modules.order.mapper.OrderMapper;
import com.heima.modules.order.service.OrderService;
import com.heima.modules.order.vo.OrderVO;
import com.heima.modules.payment.dto.PaymentDtos;
import com.heima.modules.payment.entity.TradePayment;
import com.heima.modules.payment.entity.TradePaymentNotification;
import com.heima.modules.payment.entity.TradeReconciliation;
import com.heima.modules.payment.event.PaymentCompletedEvent;
import com.heima.modules.payment.event.PaymentEventPublisher;
import com.heima.modules.payment.mapper.TradePaymentMapper;
import com.heima.modules.payment.mapper.TradePaymentNotificationMapper;
import com.heima.modules.payment.mapper.TradeReconciliationMapper;
import com.heima.modules.resource.service.ResourceTradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final TradePaymentMapper paymentMapper;
    private final TradePaymentNotificationMapper notificationMapper;
    private final TradeReconciliationMapper reconciliationMapper;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final ResourceTradeService resourceTradeService;
    private final PaymentEventPublisher eventPublisher;
    private final OrderService orderService;

    @Transactional
    public OrderVO.PaymentVO pay(String orderNo, PaymentDtos.PayRequest request, Long userId, String clientIp) {
        validateScene(request.getChannel(), request.getScene());
        Order order = lockedOrder(orderNo);
        requireOwner(order, userId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new ServiceException(409, "当前订单状态不能发起支付");
        }
        if (!order.getExpireAt().isAfter(LocalDateTime.now())) {
            throw new ServiceException(409, "订单已过期，请重新下单");
        }
        TradePayment existing = paymentMapper.selectByOrderForUpdate(order.getId());
        if (existing != null) {
            if (existing.getChannel().equals(request.getChannel()) && existing.getScene().equals(request.getScene())
                    && ("CREATED".equals(existing.getStatus()) || "PAYING".equals(existing.getStatus()))
                    && hasInstruction(existing)) {
                return paymentVO(existing);
            }
            throw new ServiceException(409, "订单已有支付单，请查单或重新下单");
        }
        Item item = itemMapper.selectOne(new LambdaQueryWrapper<Item>().eq(Item::getOrderId, order.getId()));
        if (item == null) {
            throw new ServiceException(500, "订单缺少资源明细");
        }
        TradePayment payment = new TradePayment();
        payment.setPaymentNo("P" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        payment.setOrderId(order.getId());
        payment.setChannel(request.getChannel());
        payment.setScene(request.getScene());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("CNY");
        payment.setStatus("CREATED");
        payment.setVersion(0);
        paymentMapper.insert(payment);

        PaymentChannelGateway gateway = gatewayRegistry.get(request.getChannel());
        PaymentInstruction instruction = gateway.createPayment(new PaymentCommand(payment.getPaymentNo(), order.getOrderNo(),
                item.getResourceName(), order.getTotalAmount(), request.getScene(), clientIp, order.getExpireAt()));
        payment.setFormHtml(instruction.formHtml());
        payment.setCodeUrl(instruction.codeUrl());
        payment.setH5Url(instruction.h5Url());
        payment.setStatus("PAYING");
        paymentMapper.updateById(payment);
        return paymentVO(payment);
    }

    @Transactional(readOnly = true)
    public OrderVO.PaymentVO userPayment(String orderNo, Long userId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException(404, "订单不存在");
        }
        TradePayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<TradePayment>().eq(TradePayment::getOrderId, order.getId()));
        if (payment == null) {
            throw new ServiceException(404, "支付单不存在");
        }
        return paymentVO(payment);
    }

    @Transactional
    public void handleNotification(String channel, PaymentNotification notification) {
        TradePaymentNotification existing = notificationById(notification.notificationId());
        if (existing != null) {
            return;
        }
        Order order = lockedOrder(notification.orderNo());
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        if (payment == null || !channel.equals(payment.getChannel())) {
            throw new ServiceException(400, "支付通知渠道或订单不匹配");
        }
        TradePaymentNotification record = notificationRecord(notification.notificationId(), channel, order, payment,
                notification.channelTradeNo(), notification.eventType(), notification.rawPayload());
        notificationMapper.insert(record);
        if (!notification.success()) {
            record.setProcessStatus("IGNORED");
            record.setProcessedAt(LocalDateTime.now());
            notificationMapper.updateById(record);
            return;
        }
        validatePaid(order, payment, notification.orderNo(), notification.channelTradeNo(), notification.amount(), notification.currency());
        applyPaid(order, payment, notification.channelTradeNo(), notification.eventType());
        record.setProcessStatus("PROCESSED");
        record.setProcessedAt(LocalDateTime.now());
        notificationMapper.updateById(record);
    }

    @Transactional
    public OrderVO.ReconciliationVO reconcile(String orderNo, String triggeredBy, Long operatorId) {
        Order order = lockedOrder(orderNo);
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        if (payment == null) {
            throw new ServiceException(409, "订单尚未发起支付");
        }
        String previousState = order.getStatus() + ":" + order.getGrantStatus();
        PaymentChannelGateway gateway = gatewayRegistry.get(payment.getChannel());
        ChannelPayment channelPayment = gateway.queryPayment(orderNo);
        if (!orderNo.equals(channelPayment.orderNo())) {
            throw new ServiceException(502, "渠道查单订单号不匹配");
        }
        payment.setLastQueryAt(LocalDateTime.now());
        payment.setChannelStatus(channelPayment.status());
        if (channelPayment.channelTradeNo() != null) {
            payment.setChannelTradeNo(channelPayment.channelTradeNo());
        }
        paymentMapper.updateById(payment);

        String result;
        String detail;
        if (channelPayment.paid()) {
            boolean valid = paidValuesMatch(order, payment, channelPayment.orderNo(), channelPayment.channelTradeNo(),
                    channelPayment.amount(), channelPayment.currency());
            boolean canApply = "PENDING_PAYMENT".equals(order.getStatus()) || "PAID".equals(order.getStatus());
            if (!valid || !canApply) {
                result = "MISMATCH";
                detail = !valid ? "渠道金额、币种或交易号与本地支付单不一致" : "本地订单已关闭或正在退款，未自动发放权益";
            } else {
                applyPaid(order, payment, channelPayment.channelTradeNo(), channelPayment.status());
                result = previousState.equals(order.getStatus() + ":" + order.getGrantStatus()) ? "MATCHED" : "REPAIRED";
                detail = "渠道支付成功，本地状态已核验";
            }
        } else {
            boolean localPaid = "PAID".equals(order.getStatus()) || "REFUNDING".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus());
            result = localPaid ? "MISMATCH" : "CHANNEL_PENDING";
            detail = localPaid ? "本地已支付但渠道未确认成功" : "渠道尚未确认支付成功";
        }
        TradeReconciliation reconciliation = reconciliation(order, payment, channelPayment, triggeredBy, result, detail, operatorId);
        reconciliationMapper.insert(reconciliation);
        return reconciliationVO(reconciliation, orderNo);
    }

    @Transactional
    public OrderVO close(String orderNo, Long userId, String reason) {
        Order order = lockedOrder(orderNo);
        if (userId != null) {
            requireOwner(order, userId);
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new ServiceException(409, "仅待支付订单可以关闭");
        }
        TradePayment payment = paymentMapper.selectByOrderForUpdate(order.getId());
        if (payment != null) {
            PaymentChannelGateway gateway = gatewayRegistry.get(payment.getChannel());
            ChannelPayment channelPayment = gateway.queryPayment(orderNo);
            payment.setLastQueryAt(LocalDateTime.now());
            payment.setChannelStatus(channelPayment.status());
            if (channelPayment.paid()) {
                validatePaid(order, payment, channelPayment.orderNo(), channelPayment.channelTradeNo(),
                        channelPayment.amount(), channelPayment.currency());
                applyPaid(order, payment, channelPayment.channelTradeNo(), channelPayment.status());
                paymentMapper.updateById(payment);
                return orderService.toVO(order, true);
            }
            gateway.closePayment(orderNo);
            payment.setStatus("CLOSED");
            paymentMapper.updateById(payment);
        }
        order.setStatus("CLOSED");
        order.setClosedAt(LocalDateTime.now());
        order.setCloseReason(reason);
        orderMapper.updateById(order);
        return orderService.toVO(order, true);
    }

    @Transactional
    public void recoverGrant(String orderNo) {
        Order order = lockedOrder(orderNo);
        if (!"PAID".equals(order.getStatus()) || "GRANTED".equals(order.getGrantStatus()) || "REVOKED".equals(order.getGrantStatus())) {
            return;
        }
        grant(order);
    }

    private void applyPaid(Order order, TradePayment payment, String channelTradeNo, String channelStatus) {
        TradePayment repeated = paymentMapper.selectByChannelTradeNo(payment.getChannel(), channelTradeNo);
        if (repeated != null && !repeated.getId().equals(payment.getId())) {
            throw new ServiceException(409, "渠道交易号已绑定其他支付单");
        }
        if ("REFUNDING".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus()) || "REVOKED".equals(order.getGrantStatus())) {
            return;
        }
        boolean newlyPaid = !"SUCCEEDED".equals(payment.getStatus()) || !"PAID".equals(order.getStatus());
        payment.setStatus("SUCCEEDED");
        payment.setChannelTradeNo(channelTradeNo);
        payment.setChannelStatus(channelStatus);
        payment.setPaidAt(payment.getPaidAt() == null ? LocalDateTime.now() : payment.getPaidAt());
        payment.setErrorMessage(null);
        paymentMapper.updateById(payment);
        if (newlyPaid) {
            order.setStatus("PAID");
            order.setPaidAt(order.getPaidAt() == null ? LocalDateTime.now() : order.getPaidAt());
            order.setGrantStatus("PENDING");
            orderMapper.updateById(order);
        }
        if (!"GRANTED".equals(order.getGrantStatus())) {
            grant(order);
        }
    }

    private void grant(Order order) {
        resourceTradeService.grantPaidAccess(order.getUserId(), order.getResourceId(), order.getOrderNo());
        order.setGrantStatus("GRANTED");
        orderMapper.updateById(order);
        eventPublisher.publish(new PaymentCompletedEvent(order.getOrderNo(), order.getUserId(), order.getResourceId(),
                order.getTotalAmount(), "PAID"));
    }

    private void validatePaid(Order order, TradePayment payment, String orderNo, String channelTradeNo,
                              BigDecimal amount, String currency) {
        if (!paidValuesMatch(order, payment, orderNo, channelTradeNo, amount, currency)) {
            throw new ServiceException(400, "支付通知订单、金额、币种或交易号校验失败");
        }
    }

    private boolean paidValuesMatch(Order order, TradePayment payment, String orderNo, String channelTradeNo,
                                    BigDecimal amount, String currency) {
        return order.getOrderNo().equals(orderNo) && channelTradeNo != null && !channelTradeNo.isBlank()
                && amount != null && amount.compareTo(order.getTotalAmount()) == 0
                && amount.compareTo(payment.getAmount()) == 0 && "CNY".equals(currency) && "CNY".equals(order.getCurrency());
    }

    private Order lockedOrder(String orderNo) {
        Order order = orderMapper.selectForUpdate(orderNo);
        if (order == null) {
            throw new ServiceException(404, "订单不存在");
        }
        return order;
    }

    private static void requireOwner(Order order, Long userId) {
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException(404, "订单不存在");
        }
    }

    private static void validateScene(String channel, String scene) {
        boolean valid = "ALIPAY".equals(channel) && ("PAGE".equals(scene) || "WAP".equals(scene))
                || "WECHAT".equals(channel) && ("NATIVE".equals(scene) || "H5".equals(scene));
        if (!valid) {
            throw new ServiceException(400, "支付渠道与支付场景不匹配");
        }
    }

    private static boolean hasInstruction(TradePayment payment) {
        return payment.getFormHtml() != null && !payment.getFormHtml().isBlank()
                || payment.getCodeUrl() != null && !payment.getCodeUrl().isBlank()
                || payment.getH5Url() != null && !payment.getH5Url().isBlank();
    }

    private TradePaymentNotification notificationById(String notificationId) {
        return notificationMapper.selectOne(new LambdaQueryWrapper<TradePaymentNotification>()
                .eq(TradePaymentNotification::getNotificationId, notificationId));
    }

    static TradePaymentNotification notificationRecord(String notificationId, String channel, Order order, TradePayment payment,
                                                       String channelTradeNo, String eventType, String rawPayload) {
        TradePaymentNotification record = new TradePaymentNotification();
        record.setNotificationId(notificationId);
        record.setChannel(channel);
        record.setOrderId(order.getId());
        record.setPaymentId(payment.getId());
        record.setChannelTradeNo(channelTradeNo);
        record.setEventType(eventType);
        record.setProcessStatus("RECEIVED");
        record.setRawPayload(rawPayload);
        return record;
    }

    private static TradeReconciliation reconciliation(Order order, TradePayment payment, ChannelPayment channelPayment,
                                                      String triggeredBy, String result, String detail, Long operatorId) {
        TradeReconciliation reconciliation = new TradeReconciliation();
        reconciliation.setReconciliationNo("C" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        reconciliation.setOrderId(order.getId());
        reconciliation.setPaymentId(payment.getId());
        reconciliation.setChannel(payment.getChannel());
        reconciliation.setTriggeredBy(triggeredBy);
        reconciliation.setResult(result);
        reconciliation.setExpectedAmount(order.getTotalAmount());
        reconciliation.setChannelAmount(channelPayment.amount());
        reconciliation.setExpectedStatus(order.getStatus());
        reconciliation.setChannelStatus(channelPayment.status());
        reconciliation.setChannelTradeNo(channelPayment.channelTradeNo());
        reconciliation.setOperatorId(operatorId);
        reconciliation.setDetail(detail);
        reconciliation.setReconciledAt(LocalDateTime.now());
        return reconciliation;
    }

    private static OrderVO.ReconciliationVO reconciliationVO(TradeReconciliation reconciliation, String orderNo) {
        OrderVO.ReconciliationVO vo = new OrderVO.ReconciliationVO();
        BeanUtils.copyProperties(reconciliation, vo);
        vo.setOrderNo(orderNo);
        return vo;
    }

    private static OrderVO.PaymentVO paymentVO(TradePayment payment) {
        OrderVO.PaymentVO vo = new OrderVO.PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        return vo;
    }
}
