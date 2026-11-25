package com.heima.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class OrderDtos {
    private OrderDtos() {
    }

    @Data
    @Schema(description = "创建资源购买订单")
    public static class CreateRequest {
        @NotNull
        @Schema(description = "资源ID")
        private Long resourceId;

        @NotBlank
        @Pattern(regexp = "PC|H5")
        @Schema(description = "客户端类型", allowableValues = {"PC", "H5"})
        private String clientType;

        @NotBlank
        @Size(min = 8, max = 48)
        @Schema(description = "客户端生成的幂等键，同一用户同一键只创建一单")
        private String idempotencyKey;
    }

    @Data
    @Schema(description = "订单分页查询")
    public static class Query {
        @Min(1)
        @Schema(description = "页码", defaultValue = "1")
        private long page = 1;

        @Min(1)
        @Max(100)
        @Schema(description = "每页数量，最大100", defaultValue = "20")
        private long size = 20;

        @Pattern(regexp = "PENDING_PAYMENT|PAID|CLOSED|REFUNDING|REFUNDED")
        @Schema(description = "订单状态")
        private String status;

        @Size(max = 64)
        @Schema(description = "订单号，后台可按订单号精确查询")
        private String orderNo;

        @Schema(description = "购买用户ID，仅后台可用")
        private Long userId;
    }

    @Data
    @Schema(description = "退款分页查询")
    public static class RefundQuery {
        @Min(1)
        @Schema(description = "页码", defaultValue = "1")
        private long page = 1;

        @Min(1)
        @Max(100)
        @Schema(description = "每页数量，最大100", defaultValue = "20")
        private long size = 20;

        @Pattern(regexp = "PENDING_REVIEW|APPROVED|PROCESSING|SUCCEEDED|REJECTED|ABNORMAL")
        @Schema(description = "退款状态")
        private String status;
    }

    @Data
    @Schema(description = "申请退款")
    public static class RefundRequest {
        @NotBlank
        @Size(max = 500)
        @Schema(description = "退款原因")
        private String reason;
    }

    @Data
    @Schema(description = "后台退款审核")
    public static class RefundReviewRequest {
        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT")
        @Schema(description = "审核动作", allowableValues = {"APPROVE", "REJECT"})
        private String action;

        @Size(max = 1000)
        @Schema(description = "审核意见")
        private String opinion;
    }
}
