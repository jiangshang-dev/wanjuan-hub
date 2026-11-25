package com.heima.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.heima.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@TableName("sys_admin")
public class SysAdmin extends BaseEntity {
    private String username;
    private String nickname;
    private String email;
    private String password;
    private Integer status;
    private Integer tokenVersion;
}
