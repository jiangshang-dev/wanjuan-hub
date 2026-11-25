package com.heima.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_account")
public class UserAccount extends BaseEntity {
    private String username;
    private String email;
    private String phone;
    private String password;
    private Integer status;
    private Integer tokenVersion;
}
