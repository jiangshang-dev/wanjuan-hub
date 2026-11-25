package com.heima.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.auth.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
