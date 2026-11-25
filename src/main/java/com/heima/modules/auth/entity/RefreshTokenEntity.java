package com.heima.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("auth_refresh_token")
public class RefreshTokenEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tokenId;
    private String userType;
    private Long userId;
    private String familyId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String replacedBy;
    private LocalDateTime createTime;
}
