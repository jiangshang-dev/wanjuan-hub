package com.heima.modules.payment.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(String orderNo, Long userId, Long resourceId, BigDecimal amount, String action) {
}
