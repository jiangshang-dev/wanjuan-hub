package com.heima.modules.statistics.service;

import com.heima.modules.statistics.vo.StatisticsVOs.DailyStatisticsVO;
import com.heima.modules.statistics.vo.StatisticsVOs.DashboardVO;
import java.time.LocalDate;

public interface StatisticsService {
    DashboardVO dashboard(int days);
    DailyStatisticsVO refreshDaily(LocalDate statDate);
}
