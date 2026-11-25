package com.heima.modules.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trade_payment")
@EqualsAndHashCode(callSuper = true)
public class TradePayment extends BaseEntity {
    private String paymentNo;
    private Long orderId;
    private String channel;
    private String scene;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String channelTradeNo;
    private String formHtml;
    private String codeUrl;
    private String h5Url;
    private String channelStatus;
    private LocalDateTime paidAt;
    private LocalDateTime lastQueryAt;
    private String errorMessage;
    private Integer version;
}
