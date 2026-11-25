package com.heima.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_log")
public class LoginLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userType;
    private Long userId;
    private String account;
    private String loginType;
    private String ip;
    private String location;
    private Boolean success;
    private String message;
    private String traceId;
    private LocalDateTime createTime;
}
