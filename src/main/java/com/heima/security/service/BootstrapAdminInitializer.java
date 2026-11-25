package com.heima.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.modules.system.entity.SysAdmin;
import com.heima.modules.system.entity.SysAdminRole;
import com.heima.modules.system.mapper.SysAdminMapper;
import com.heima.modules.system.mapper.SysAdminRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final Environment environment;
    private final SysAdminMapper adminMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @Override public void run(ApplicationArguments args) {
        String username=environment.getProperty("BOOTSTRAP_ADMIN_USERNAME"); String password=environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD");
        if ((username==null||username.isBlank())&&(password==null||password.isBlank())) return;
        if (username==null||username.isBlank()||password==null||password.length()<8||password.length()>72) throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME and a 8-72 character BOOTSTRAP_ADMIN_PASSWORD must be provided together");
        if (adminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername,username))>0) { log.info("Bootstrap admin already exists; credentials were not changed"); return; }
        transactionTemplate.executeWithoutResult(tx->{
            SysAdmin admin=new SysAdmin(); admin.setUsername(username); admin.setNickname("系统管理员"); admin.setPassword(passwordEncoder.encode(password)); admin.setStatus(1); admin.setTokenVersion(0); adminMapper.insert(admin);
            SysAdminRole relation=new SysAdminRole(); relation.setAdminId(admin.getId()); relation.setRoleId(1L); relation.setCreateTime(LocalDateTime.now()); adminRoleMapper.insert(relation);
        });
        log.info("Bootstrap admin initialized from environment");
    }
}
