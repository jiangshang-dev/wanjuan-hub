package com.heima.modules.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.resource.entity.ResourcePointAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ResourcePointAccountMapper extends BaseMapper<ResourcePointAccount> {
    @Insert("INSERT IGNORE INTO resource_point_account(user_id, balance) VALUES(#{userId}, 0)")
    int createIgnore(@Param("userId") Long userId);

    @Select("SELECT * FROM resource_point_account WHERE user_id = #{userId} FOR UPDATE")
    ResourcePointAccount selectForUpdate(@Param("userId") Long userId);

    @Update("UPDATE resource_point_account SET balance = balance - #{points} WHERE user_id = #{userId} AND balance >= #{points}")
    int deduct(@Param("userId") Long userId, @Param("points") Integer points);
}
