package com.heima.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private String nickname;
    private String avatar;
    private Long avatarFileId;
    private String signature;
    private String pendingNickname;
    private Long pendingAvatarFileId;
    private String pendingSignature;
    private Boolean pendingRemoveAvatar;
    private String profileAuditStatus;
    private String profileAuditReason;
    private Integer profileVersion;
    private LocalDate birthday;
    private Integer gender;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
