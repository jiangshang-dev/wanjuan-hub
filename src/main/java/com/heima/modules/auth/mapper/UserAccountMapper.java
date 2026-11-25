package com.heima.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
    @Update("UPDATE user_account SET token_version=token_version+1,status=#{status} WHERE id=#{id}")
    int updateStatusAndVersion(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE user_account SET token_version=token_version+1 WHERE id=#{id}")
    int incrementTokenVersion(@Param("id") Long id);
}
