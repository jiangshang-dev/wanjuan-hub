package com.heima.modules.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StatisticsVOs {
    private StatisticsVOs() {}

    @Data @NoArgsConstructor @AllArgsConstructor @Schema(description = "工作台实时汇总")
    public static class SummaryVO {
        @Schema(description = "注册用户数") private Long totalUsers;
        @Schema(description = "今日新增用户数") private Long newUsersToday;
        @Schema(description = "当前公开内容数，含博客、资源、论坛主题") private Long publicContents;
        @Schema(description = "待处理审核数，含待审核、处理中、人工审核和失败") private Long pendingAudits;
        @Schema(description = "订单总数") private Long totalOrders;
        @Schema(description = "今日新增订单数") private Long newOrdersToday;
        @Schema(description = "成功支付总额，按支付成功时间统计，不扣退款") private BigDecimal totalPaymentAmount;
        @Schema(description = "今日成功支付金额，按支付成功时间统计，不扣退款") private BigDecimal todayPaymentAmount;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Schema(description = "日趋势数据")
    public static class TrendVO {
        @Schema(description = "统计日期") private LocalDate statDate;
        @Schema(description = "新增用户") private Long newUsers;
        @Schema(description = "新增内容，按内容创建时间统计") private Long newContents;
        @Schema(description = "完成审核任务数，按完成时间统计") private Long auditCount;
        @Schema(description = "新增订单") private Long orderCount;
        @Schema(description = "成功支付金额") private BigDecimal paymentAmount;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Schema(description = "公开内容分类统计")
    public static class CategoryStatVO {
        @Schema(description = "内容类型：BLOG、RESOURCE、FORUM_TOPIC") private String contentType;
        @Schema(description = "分类或板块ID，字符串") private String categoryId;
        @Schema(description = "分类或板块名称") private String categoryName;
        @Schema(description = "公开内容数量") private Long contentCount;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Schema(description = "消息与Outbox积压")
    public static class BacklogVO {
        @Schema(description = "未完成MQ消费记录数，状态非SUCCESS") private Long mqConsumeBacklog;
        @Schema(description = "待发送或发送失败的Outbox记录数") private Long outboxBacklog;
    }

    @Schema(description = "后台工作台完整数据")
    public record DashboardVO(
            @Schema(description = "实时汇总") SummaryVO summary,
            @Schema(description = "指定天数趋势，缺失日期补零") List<TrendVO> trends,
            @Schema(description = "公开内容分类统计，最多100项") List<CategoryStatVO> categories,
            @Schema(description = "消息积压") BacklogVO backlog) {}

    @Schema(description = "每日汇总结果")
    public record DailyStatisticsVO(
            @Schema(description = "记录ID，字符串") String id,
            @Schema(description = "统计日期") LocalDate statDate,
            @Schema(description = "新增用户") Long newUsers,
            @Schema(description = "新增内容") Long newContents,
            @Schema(description = "完成审核数") Long auditCount,
            @Schema(description = "新增订单数") Long orderCount,
            @Schema(description = "成功支付金额") BigDecimal orderAmount) {}
}
