package com.heima.modules.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class StatisticsEntities {
    private StatisticsEntities() {}

    @Data
    @TableName("statistics_daily")
    @EqualsAndHashCode(callSuper = true)
    public static class Daily extends BaseEntity {
        private LocalDate statDate;
        private Long newUsers;
        private Long newContents;
        private Long auditCount;
        private Long orderCount;
        private BigDecimal orderAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private LocalDate statDate;
        private Long metricValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAmount {
        private LocalDate statDate;
        private BigDecimal metricValue;
    }
}
