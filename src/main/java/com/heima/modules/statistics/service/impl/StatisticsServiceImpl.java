package com.heima.modules.statistics.service.impl;

import com.heima.exception.ServiceException;
import com.heima.modules.statistics.entity.StatisticsEntities.Daily;
import com.heima.modules.statistics.entity.StatisticsEntities.DailyAmount;
import com.heima.modules.statistics.entity.StatisticsEntities.DailyCount;
import com.heima.modules.statistics.mapper.StatisticsMapper;
import com.heima.modules.statistics.service.StatisticsService;
import com.heima.modules.statistics.vo.StatisticsVOs.DailyStatisticsVO;
import com.heima.modules.statistics.vo.StatisticsVOs.DashboardVO;
import com.heima.modules.statistics.vo.StatisticsVOs.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper statisticsMapper;

    @Override
    public DashboardVO dashboard(int days) {
        int boundedDays = Math.min(31, Math.max(1, days));
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime start = today.minusDays(boundedDays - 1L).atStartOfDay();

        Map<LocalDate, Long> users = counts(statisticsMapper.selectUserTrend(start, tomorrowStart));
        Map<LocalDate, Long> contents = counts(statisticsMapper.selectContentTrend(start, tomorrowStart));
        Map<LocalDate, Long> audits = counts(statisticsMapper.selectAuditTrend(start, tomorrowStart));
        Map<LocalDate, Long> orders = counts(statisticsMapper.selectOrderTrend(start, tomorrowStart));
        Map<LocalDate, BigDecimal> payments = statisticsMapper.selectPaymentTrend(start, tomorrowStart).stream()
                .collect(Collectors.toMap(DailyAmount::getStatDate, DailyAmount::getMetricValue));

        List<TrendVO> trends = today.minusDays(boundedDays - 1L).datesUntil(today.plusDays(1))
                .map(date -> new TrendVO(date, users.getOrDefault(date, 0L), contents.getOrDefault(date, 0L),
                        audits.getOrDefault(date, 0L), orders.getOrDefault(date, 0L),
                        payments.getOrDefault(date, BigDecimal.ZERO))).toList();
        return new DashboardVO(statisticsMapper.selectSummary(todayStart, tomorrowStart), trends,
                statisticsMapper.selectCategoryStats(), statisticsMapper.selectBacklog());
    }

    @Override
    @Transactional
    public DailyStatisticsVO refreshDaily(LocalDate statDate) {
        LocalDate target = statDate == null ? LocalDate.now().minusDays(1) : statDate;
        if (target.isAfter(LocalDate.now())) throw new ServiceException("不能汇总未来日期");
        statisticsMapper.upsertDaily(target, target.atStartOfDay(), target.plusDays(1).atStartOfDay());
        return dailyVO(statisticsMapper.selectDaily(target));
    }

    private static Map<LocalDate, Long> counts(List<DailyCount> values) {
        return values.stream().collect(Collectors.toMap(DailyCount::getStatDate, DailyCount::getMetricValue));
    }

    private static DailyStatisticsVO dailyVO(Daily daily) {
        if (daily == null) throw new ServiceException(500, "每日汇总写入后读取失败");
        return new DailyStatisticsVO(String.valueOf(daily.getId()), daily.getStatDate(), daily.getNewUsers(),
                daily.getNewContents(), daily.getAuditCount(), daily.getOrderCount(), daily.getOrderAmount());
    }
}
