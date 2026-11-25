package com.heima.modules.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsDailyTask {
    private final StatisticsService statisticsService;

    @Scheduled(cron = "${wanjuan.statistics.daily-cron:0 15 0 * * *}")
    public void summarizeYesterday() {
        try {
            statisticsService.refreshDaily(LocalDate.now().minusDays(1));
        } catch (RuntimeException ex) {
            log.error("每日统计汇总失败，将在下次调度重试", ex);
        }
    }
}
