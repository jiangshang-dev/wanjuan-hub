package com.heima.modules.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.points.entity.PointsEntities.Account;
import com.heima.modules.points.entity.PointsEntities.Level;
import com.heima.modules.points.entity.PointsEntities.Record;
import com.heima.modules.points.entity.PointsEntities.Rule;
import com.heima.modules.points.entity.PointsEntities.SignIn;
import com.heima.modules.points.entity.PointsEntities.StreakRule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public final class PointsMappers {
    private PointsMappers() {}

    @Mapper
    public interface AccountMapper extends BaseMapper<Account> {
        @Insert("INSERT IGNORE INTO points_account(user_id) VALUES(#{userId})")
        int insertIgnore(@Param("userId") Long userId);

        @Select("SELECT * FROM points_account WHERE user_id=#{userId} FOR UPDATE")
        Account selectForUpdate(@Param("userId") Long userId);

        @Update("UPDATE points_account SET balance=balance-#{points},version=version+1 WHERE user_id=#{userId} AND balance>=#{points}")
        int deduct(@Param("userId") Long userId, @Param("points") int points);
    }

    @Mapper
    public interface RecordMapper extends BaseMapper<Record> {
        @Select("SELECT COALESCE(SUM(change_amount),0) FROM points_record WHERE user_id=#{userId} AND biz_type=#{bizType} AND change_amount>0 AND create_time>=#{start} AND create_time<#{end}")
        int sumDailyEarned(@Param("userId") Long userId, @Param("bizType") String bizType,
                           @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    }

    @Mapper public interface RuleMapper extends BaseMapper<Rule> {}
    @Mapper public interface SignInMapper extends BaseMapper<SignIn> {}
    @Mapper public interface StreakRuleMapper extends BaseMapper<StreakRule> {}
    @Mapper public interface LevelMapper extends BaseMapper<Level> {}
}
