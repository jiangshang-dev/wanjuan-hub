package com.heima.modules.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.statistics.entity.StatisticsEntities.Daily;
import com.heima.modules.statistics.entity.StatisticsEntities.DailyAmount;
import com.heima.modules.statistics.entity.StatisticsEntities.DailyCount;
import com.heima.modules.statistics.vo.StatisticsVOs.BacklogVO;
import com.heima.modules.statistics.vo.StatisticsVOs.CategoryStatVO;
import com.heima.modules.statistics.vo.StatisticsVOs.SummaryVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatisticsMapper extends BaseMapper<Daily> {
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM user_account) AS total_users,
              (SELECT COUNT(*) FROM user_account WHERE create_time>=#{todayStart} AND create_time<#{tomorrowStart}) AS new_users_today,
              (SELECT COUNT(*) FROM blog_article WHERE deleted=0 AND status!='OFFLINE' AND published_version_no IS NOT NULL)
              + (SELECT COUNT(*) FROM resource_info WHERE deleted=0 AND online=1 AND invalid=0 AND published_version_no IS NOT NULL)
              + (SELECT COUNT(*) FROM forum_topic WHERE deleted=0 AND status!='OFFLINE' AND published_version_no IS NOT NULL) AS public_contents,
              (SELECT COUNT(*) FROM audit_task WHERE status IN ('PENDING','PROCESSING','MANUAL_REVIEW','FAILED')) AS pending_audits,
              (SELECT COUNT(*) FROM trade_order) AS total_orders,
              (SELECT COUNT(*) FROM trade_order WHERE create_time>=#{todayStart} AND create_time<#{tomorrowStart}) AS new_orders_today,
              (SELECT COALESCE(SUM(amount),0) FROM trade_payment WHERE status='SUCCEEDED' AND paid_at IS NOT NULL) AS total_payment_amount,
              (SELECT COALESCE(SUM(amount),0) FROM trade_payment WHERE status='SUCCEEDED' AND paid_at>=#{todayStart} AND paid_at<#{tomorrowStart}) AS today_payment_amount
            """)
    SummaryVO selectSummary(@Param("todayStart") LocalDateTime todayStart,
                            @Param("tomorrowStart") LocalDateTime tomorrowStart);

    @Select("SELECT DATE(create_time) AS stat_date,COUNT(*) AS metric_value FROM user_account WHERE create_time>=#{start} AND create_time<#{end} GROUP BY DATE(create_time)")
    List<DailyCount> selectUserTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT stat_date,SUM(metric_value) AS metric_value FROM (
              SELECT DATE(create_time) AS stat_date,COUNT(*) AS metric_value FROM blog_article
              WHERE create_time>=#{start} AND create_time<#{end} GROUP BY DATE(create_time)
              UNION ALL
              SELECT DATE(create_time) AS stat_date,COUNT(*) AS metric_value FROM resource_info
              WHERE create_time>=#{start} AND create_time<#{end} GROUP BY DATE(create_time)
              UNION ALL
              SELECT DATE(create_time) AS stat_date,COUNT(*) AS metric_value FROM forum_topic
              WHERE create_time>=#{start} AND create_time<#{end} GROUP BY DATE(create_time)
            ) daily_content GROUP BY stat_date
            """)
    List<DailyCount> selectContentTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(finished_at) AS stat_date,COUNT(*) AS metric_value FROM audit_task WHERE finished_at>=#{start} AND finished_at<#{end} GROUP BY DATE(finished_at)")
    List<DailyCount> selectAuditTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(create_time) AS stat_date,COUNT(*) AS metric_value FROM trade_order WHERE create_time>=#{start} AND create_time<#{end} GROUP BY DATE(create_time)")
    List<DailyCount> selectOrderTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(paid_at) AS stat_date,COALESCE(SUM(amount),0) AS metric_value FROM trade_payment WHERE status='SUCCEEDED' AND paid_at>=#{start} AND paid_at<#{end} GROUP BY DATE(paid_at)")
    List<DailyAmount> selectPaymentTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT content_type,category_id,category_name,content_count FROM (
              SELECT 'BLOG' AS content_type,CAST(c.id AS CHAR) AS category_id,c.name AS category_name,COUNT(a.id) AS content_count
              FROM blog_category c LEFT JOIN blog_article a ON a.category_id=c.id AND a.deleted=0
                AND a.status!='OFFLINE' AND a.published_version_no IS NOT NULL
              GROUP BY c.id,c.name
              UNION ALL
              SELECT 'RESOURCE' AS content_type,CAST(c.id AS CHAR) AS category_id,c.name AS category_name,COUNT(i.id) AS content_count
              FROM resource_category c LEFT JOIN resource_info i ON i.category_id=c.id AND i.deleted=0 AND i.online=1
                AND i.invalid=0 AND i.published_version_no IS NOT NULL
              GROUP BY c.id,c.name
              UNION ALL
              SELECT 'FORUM_TOPIC' AS content_type,CAST(b.id AS CHAR) AS category_id,b.name AS category_name,COUNT(t.id) AS content_count
              FROM forum_board b LEFT JOIN forum_topic t ON t.board_id=b.id AND t.deleted=0
                AND t.status!='OFFLINE' AND t.published_version_no IS NOT NULL
              GROUP BY b.id,b.name
            ) category_stats ORDER BY content_count DESC,content_type,category_id LIMIT 100
            """)
    List<CategoryStatVO> selectCategoryStats();

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mq_consume_log WHERE status!='SUCCESS') AS mq_consume_backlog,
              (SELECT COUNT(*) FROM audit_outbox WHERE status IN ('PENDING','FAILED')) AS outbox_backlog
            """)
    BacklogVO selectBacklog();

    @Select("SELECT * FROM statistics_daily WHERE stat_date=#{statDate}")
    Daily selectDaily(@Param("statDate") LocalDate statDate);

    @Insert("""
            INSERT INTO statistics_daily(stat_date,new_users,new_contents,audit_count,order_count,order_amount)
            SELECT #{statDate},
              (SELECT COUNT(*) FROM user_account WHERE create_time>=#{start} AND create_time<#{end}),
              (SELECT SUM(content_count) FROM (
                SELECT COUNT(*) AS content_count FROM blog_article WHERE create_time>=#{start} AND create_time<#{end}
                UNION ALL SELECT COUNT(*) FROM resource_info WHERE create_time>=#{start} AND create_time<#{end}
                UNION ALL SELECT COUNT(*) FROM forum_topic WHERE create_time>=#{start} AND create_time<#{end}
              ) daily_content),
              (SELECT COUNT(*) FROM audit_task WHERE finished_at>=#{start} AND finished_at<#{end}),
              (SELECT COUNT(*) FROM trade_order WHERE create_time>=#{start} AND create_time<#{end}),
              (SELECT COALESCE(SUM(amount),0) FROM trade_payment WHERE status='SUCCEEDED' AND paid_at>=#{start} AND paid_at<#{end})
            ON DUPLICATE KEY UPDATE new_users=VALUES(new_users),new_contents=VALUES(new_contents),
              audit_count=VALUES(audit_count),order_count=VALUES(order_count),order_amount=VALUES(order_amount)
            """)
    int upsertDaily(@Param("statDate") LocalDate statDate, @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);
}
