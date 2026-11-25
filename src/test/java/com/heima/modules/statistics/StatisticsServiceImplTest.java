package com.heima.modules.statistics;

import com.heima.exception.ServiceException;
import com.heima.modules.statistics.entity.StatisticsEntities.Daily;
import com.heima.modules.statistics.entity.StatisticsEntities.DailyAmount;
import com.heima.modules.statistics.entity.StatisticsEntities.DailyCount;
import com.heima.modules.statistics.mapper.StatisticsMapper;
import com.heima.modules.statistics.service.impl.StatisticsServiceImpl;
import com.heima.modules.statistics.vo.StatisticsVOs.BacklogVO;
import com.heima.modules.statistics.vo.StatisticsVOs.CategoryStatVO;
import com.heima.modules.statistics.vo.StatisticsVOs.DashboardVO;
import com.heima.modules.statistics.vo.StatisticsVOs.SummaryVO;
import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {
    @Mock private StatisticsMapper statisticsMapper;
    @InjectMocks private StatisticsServiceImpl statisticsService;

    @Test
    void dashboardUsesRealMetricsAndFillsMissingTrendDatesWithZero() {
        LocalDate today = LocalDate.now();
        SummaryVO summary = new SummaryVO(20L, 3L, 9L, 2L, 8L, 1L,
                new BigDecimal("100.00"), new BigDecimal("88.50"));
        BacklogVO backlog = new BacklogVO(4L, 5L);
        List<CategoryStatVO> categories = List.of(new CategoryStatVO("BLOG", "1", "技术", 6L));
        when(statisticsMapper.selectSummary(any(), any())).thenReturn(summary);
        when(statisticsMapper.selectUserTrend(any(), any())).thenReturn(List.of(new DailyCount(today, 3L)));
        when(statisticsMapper.selectContentTrend(any(), any())).thenReturn(List.of());
        when(statisticsMapper.selectAuditTrend(any(), any())).thenReturn(List.of(new DailyCount(today, 2L)));
        when(statisticsMapper.selectOrderTrend(any(), any())).thenReturn(List.of());
        when(statisticsMapper.selectPaymentTrend(any(), any())).thenReturn(List.of(new DailyAmount(today, new BigDecimal("88.50"))));
        when(statisticsMapper.selectCategoryStats()).thenReturn(categories);
        when(statisticsMapper.selectBacklog()).thenReturn(backlog);

        DashboardVO dashboard = statisticsService.dashboard(7);

        assertEquals(7, dashboard.trends().size());
        assertEquals(today.minusDays(6), dashboard.trends().getFirst().getStatDate());
        assertEquals(0L, dashboard.trends().getFirst().getNewUsers());
        assertEquals(3L, dashboard.trends().getLast().getNewUsers());
        assertEquals(2L, dashboard.trends().getLast().getAuditCount());
        assertEquals(new BigDecimal("88.50"), dashboard.trends().getLast().getPaymentAmount());
        assertEquals(9L, dashboard.summary().getPublicContents());
        assertEquals(categories, dashboard.categories());
        assertEquals(5L, dashboard.backlog().getOutboxBacklog());
    }

    @Test
    void dailyRefreshRepeatedlyUsesIdempotentUpsertForSameDate() {
        LocalDate target = LocalDate.now().minusDays(1);
        Daily daily = new Daily();
        daily.setId(10L);
        daily.setStatDate(target);
        daily.setNewUsers(2L);
        daily.setNewContents(3L);
        daily.setAuditCount(4L);
        daily.setOrderCount(5L);
        daily.setOrderAmount(new BigDecimal("66.00"));
        when(statisticsMapper.selectDaily(target)).thenReturn(daily);

        statisticsService.refreshDaily(target);
        statisticsService.refreshDaily(target);

        verify(statisticsMapper, times(2)).upsertDaily(target, target.atStartOfDay(), target.plusDays(1).atStartOfDay());
    }

    @Test
    void futureDailyRefreshIsRejected() {
        assertThrows(ServiceException.class, () -> statisticsService.refreshDaily(LocalDate.now().plusDays(1)));
        verify(statisticsMapper, never()).upsertDaily(any(), any(), any());
    }

    @Test
    void dailyAggregationUsesPaymentSuccessTimeAndDatabaseUpsert() throws NoSuchMethodException {
        String sql = String.join(" ", StatisticsMapper.class.getMethod("upsertDaily",
                LocalDate.class, LocalDateTime.class, LocalDateTime.class).getAnnotation(Insert.class).value());

        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
        assertTrue(sql.contains("status='SUCCEEDED'"));
        assertTrue(sql.contains("paid_at>=#{start}"));
        assertTrue(sql.contains("paid_at<#{end}"));
    }
}
