package com.heima.modules.membership.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.membership.entity.MembershipEntities.CommissionConfig;
import com.heima.modules.membership.entity.MembershipEntities.DailyUsage;
import com.heima.modules.membership.entity.MembershipEntities.Plan;
import com.heima.modules.membership.entity.MembershipEntities.ResourceClaim;
import com.heima.modules.membership.entity.MembershipEntities.UserMembership;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

public final class MembershipMappers {
    private MembershipMappers() {
    }

    public interface PlanMapper extends BaseMapper<Plan> {
    }

    public interface UserMembershipMapper extends BaseMapper<UserMembership> {
        @Insert("INSERT IGNORE INTO user_membership(user_id, daily_resource_limit, version) VALUES(#{userId}, 0, 0)")
        int createIgnore(@Param("userId") Long userId);

        @Select("SELECT * FROM user_membership WHERE user_id=#{userId} FOR UPDATE")
        UserMembership selectForUpdate(@Param("userId") Long userId);
    }

    public interface DailyUsageMapper extends BaseMapper<DailyUsage> {
        @Insert("INSERT IGNORE INTO membership_daily_usage(user_id, claim_date, used_count) VALUES(#{userId}, #{claimDate}, 0)")
        int createIgnore(@Param("userId") Long userId, @Param("claimDate") LocalDate claimDate);

        @Select("SELECT * FROM membership_daily_usage WHERE user_id=#{userId} AND claim_date=#{claimDate} FOR UPDATE")
        DailyUsage selectForUpdate(@Param("userId") Long userId, @Param("claimDate") LocalDate claimDate);

        @Update("UPDATE membership_daily_usage SET used_count=used_count+1 WHERE user_id=#{userId} AND claim_date=#{claimDate} AND used_count<#{dailyLimit}")
        int consume(@Param("userId") Long userId, @Param("claimDate") LocalDate claimDate, @Param("dailyLimit") Integer dailyLimit);
    }

    public interface ResourceClaimMapper extends BaseMapper<ResourceClaim> {
    }

    public interface CommissionConfigMapper extends BaseMapper<CommissionConfig> {
    }
}
