package com.heima.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.auth.entity.RefreshTokenEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshTokenEntity> {
    @Update("UPDATE auth_refresh_token SET revoked_at=#{now},replaced_by=#{newTokenId} WHERE token_id=#{tokenId} AND revoked_at IS NULL")
    int rotate(@Param("tokenId") String tokenId, @Param("newTokenId") String newTokenId, @Param("now") LocalDateTime now);

    @Update("UPDATE auth_refresh_token SET revoked_at=#{now} WHERE family_id=#{familyId} AND revoked_at IS NULL")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now);

    @Update("UPDATE auth_refresh_token SET revoked_at=#{now} WHERE user_type=#{userType} AND user_id=#{userId} AND revoked_at IS NULL")
    int revokeAll(@Param("userType") String userType, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Delete("DELETE FROM auth_refresh_token WHERE user_type=#{userType} AND user_id=#{userId}")
    int deleteAll(@Param("userType") String userType, @Param("userId") Long userId);
}
