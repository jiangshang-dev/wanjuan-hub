package com.heima.modules.resource.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface ResourceTradeService {
    record PurchaseItem(Long resourceId, String name, BigDecimal price, Long ownerId) {
    }

    PurchaseItem getPaidPurchase(Long resourceId, Long buyerId);

    @Transactional(propagation = Propagation.REQUIRED)
    void grantPaidAccess(Long userId, Long resourceId, String orderNo);

    @Transactional(propagation = Propagation.REQUIRED)
    void revokePaidAccess(Long userId, Long resourceId, String reason);

    void redeemPoints(Long userId, Long resourceId);
}
