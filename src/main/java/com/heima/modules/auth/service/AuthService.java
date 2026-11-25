package com.heima.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.location.IpLocationService;
import com.heima.infrastructure.redis.RedisRateLimiter;
import com.heima.modules.auth.dto.AuthDtos.*;
import com.heima.modules.auth.entity.*;
import com.heima.modules.auth.mapper.*;
import com.heima.modules.file.entity.FileObject;
import com.heima.modules.file.service.FileService;
import com.heima.modules.system.entity.SysAdmin;
import com.heima.modules.system.mapper.SysAdminMapper;
import com.heima.security.config.SecurityProperties;
import com.heima.security.service.RefreshTokenService;
import com.heima.security.token.JwtService;
import com.heima.utils.IpUtils;
import com.heima.utils.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserAccountMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final SysAdminMapper adminMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final VerificationCodeService codeService;
    private final RedisRateLimiter limiter;
    private final RefreshTokenService refreshService;
    private final JwtService jwt;
    private final SecurityProperties securityProperties;
    private final IpLocationService locationService;
    private final FileService fileService;

    public CaptchaResponse captcha() { return captchaService.create(ip()); }

    public void sendEmailCode(EmailCodeRequest request) {
        limiter.check("send-code:ip:"+ip(),20,Duration.ofMinutes(10));
        captchaService.verify(request.captchaId(),request.captchaCode());
        codeService.sendEmail(request.scene(),request.email().toLowerCase(Locale.ROOT),ip());
    }

    public void sendSmsCode(SmsCodeRequest request) {
        limiter.check("send-code:ip:"+ip(),20,Duration.ofMinutes(10));
        captchaService.verify(request.captchaId(),request.captchaCode());
        codeService.sendSms(request.scene(),request.phone(),ip());
    }

    @Transactional
    public void register(RegisterRequest request) {
        limiter.check("register:ip:"+ip(),10,Duration.ofHours(1));
        String email=request.email().toLowerCase(Locale.ROOT);
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUsername,request.username()))>0) throw new ServiceException(409,"用户名已存在");
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail,email))>0) throw new ServiceException(409,"邮箱已注册");
        codeService.consumeEmail("REGISTER",email,request.emailCode());
        UserAccount user=new UserAccount(); user.setUsername(request.username()); user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password())); user.setStatus(1); user.setTokenVersion(0); userMapper.insert(user);
        UserProfile profile=new UserProfile(); profile.setUserId(user.getId()); profile.setNickname(request.username()); profile.setGender(0);
        profile.setCreateTime(LocalDateTime.now()); profile.setUpdateTime(LocalDateTime.now()); profileMapper.insert(profile);
    }

    public TokenResponse login(LoginRequest request) {
        limiter.check("login:ip:"+ip(),30,Duration.ofMinutes(10)); limiter.checkFailures("login:"+request.account(),5);
        captchaService.verify(request.captchaId(),request.captchaCode());
        try {
            UserAccount user=userMapper.selectOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUsername,request.account())
                    .or().eq(UserAccount::getEmail,request.account().toLowerCase(Locale.ROOT)).or().eq(UserAccount::getPhone,request.account()));
            if (user==null || !passwordEncoder.matches(request.password(),user.getPassword())) throw new ServiceException(401,"账号或密码错误");
            if (user.getStatus()!=1) throw new ServiceException(403,"账号已被停用");
            limiter.clearFailures("login:"+request.account()); logLogin("USER",user.getId(),request.account(),"PASSWORD",true,null);
            return tokens(refreshService.issue("USER",user.getId(),user.getTokenVersion()));
        } catch (ServiceException e) {
            limiter.failure("login:"+request.account(),5,Duration.ofMinutes(15)); logLogin("USER",null,request.account(),"PASSWORD",false,e.getMessage()); throw e;
        }
    }

    public TokenResponse loginByEmail(EmailLoginRequest request) {
        String email=request.email().toLowerCase(Locale.ROOT); limiter.check("email-login:ip:"+ip(),20,Duration.ofMinutes(10));
        limiter.checkFailures("email-login:"+email,5); captchaService.verify(request.captchaId(),request.captchaCode());
        try {
            codeService.consumeEmail("LOGIN",email,request.emailCode());
            UserAccount user=requiredUser(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail,email),"邮箱未注册，请先注册");
            limiter.clearFailures("email-login:"+email); logLogin("USER",user.getId(),email,"EMAIL",true,null);
            return tokens(refreshService.issue("USER",user.getId(),user.getTokenVersion()));
        } catch (ServiceException e) { limiter.failure("email-login:"+email,5,Duration.ofMinutes(15)); logLogin("USER",null,email,"EMAIL",false,e.getMessage()); throw e; }
    }

    public TokenResponse loginByPhone(PhoneLoginRequest request) {
        limiter.check("phone-login:ip:"+ip(),20,Duration.ofMinutes(10)); limiter.checkFailures("phone-login:"+request.phone(),5);
        captchaService.verify(request.captchaId(),request.captchaCode());
        try {
            codeService.consumeSms("LOGIN",request.phone(),request.smsCode());
            UserAccount user=requiredUser(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getPhone,request.phone()),"手机号未注册，请先绑定");
            limiter.clearFailures("phone-login:"+request.phone()); logLogin("USER",user.getId(),request.phone(),"SMS",true,null);
            return tokens(refreshService.issue("USER",user.getId(),user.getTokenVersion()));
        } catch (ServiceException e) { limiter.failure("phone-login:"+request.phone(),5,Duration.ofMinutes(15)); logLogin("USER",null,request.phone(),"SMS",false,e.getMessage()); throw e; }
    }

    public TokenResponse adminLogin(LoginRequest request) {
        limiter.check("admin-login:ip:"+ip(),20,Duration.ofMinutes(10)); limiter.checkFailures("admin-login:"+request.account(),5);
        captchaService.verify(request.captchaId(),request.captchaCode());
        try {
            SysAdmin admin=adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername,request.account()).or().eq(SysAdmin::getEmail,request.account()));
            if (admin==null || !passwordEncoder.matches(request.password(),admin.getPassword())) throw new ServiceException(401,"账号或密码错误");
            if (admin.getStatus()!=1) throw new ServiceException(403,"管理员已被停用");
            limiter.clearFailures("admin-login:"+request.account()); logLogin("ADMIN",admin.getId(),request.account(),"PASSWORD",true,null);
            return tokens(refreshService.issue("ADMIN",admin.getId(),admin.getTokenVersion()));
        } catch (ServiceException e) { limiter.failure("admin-login:"+request.account(),5,Duration.ofMinutes(15)); logLogin("ADMIN",null,request.account(),"PASSWORD",false,e.getMessage()); throw e; }
    }

    public TokenResponse refresh(RefreshRequest request,String type) { return tokens(refreshService.rotate(request.refreshToken(),type)); }
    public void logout(LogoutRequest request,String type) { refreshService.logout(request.refreshToken(),type); }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email=request.email().toLowerCase(Locale.ROOT); limiter.check("password-reset:ip:"+ip(),5,Duration.ofHours(1));
        codeService.consumeEmail("RESET",email,request.emailCode());
        UserAccount user=requiredUser(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail,email),"邮箱未注册");
        user.setPassword(passwordEncoder.encode(request.newPassword())); userMapper.updateById(user); refreshService.revokeAll("USER",user.getId());
    }

    public MeResponse me() {
        Long id=UserUtil.requireFrontUser().getId(); UserAccount user=userMapper.selectById(id); UserProfile p=profileMapper.selectById(id);
        String avatar=avatarUrl(p.getAvatarFileId(),p.getAvatar());
        String pendingAvatar=Boolean.TRUE.equals(p.getPendingRemoveAvatar())?null:avatarUrl(p.getPendingAvatarFileId(),null);
        return new MeResponse(user.getId(),user.getUsername(),user.getEmail(),user.getPhone(),
                p.getNickname(),avatar,p.getAvatarFileId(),p.getSignature(),p.getBirthday(),p.getGender(),
                p.getPendingNickname(),pendingAvatar,p.getPendingAvatarFileId(),p.getPendingSignature(),
                p.getPendingRemoveAvatar(),p.getProfileAuditStatus(),p.getProfileAuditReason(),
                p.getAllowLike() == null || p.getAllowLike(), p.getAllowDislike() == null || p.getAllowDislike());
    }

    private String avatarUrl(Long fileId,String fallback) {
        if (fileId==null) return fallback;
        try {
            FileObject file=fileService.getRequired(fileId);
            return fileService.presign(file,java.time.Duration.ofMinutes(5));
        } catch (RuntimeException e) {
            log.warn("Unable to create avatar access URL, fileId={}",fileId);
            return fallback;
        }
    }

    private UserAccount requiredUser(LambdaQueryWrapper<UserAccount> wrapper,String message) {
        UserAccount user=userMapper.selectOne(wrapper); if (user==null) throw new ServiceException(404,message);
        if (user.getStatus()!=1) throw new ServiceException(403,"账号已被停用"); return user;
    }
    private TokenResponse tokens(RefreshTokenService.IssuedTokens issued) { return new TokenResponse(issued.accessToken(),issued.refreshToken(),"Bearer",jwt.accessExpiresIn()); }
    private String ip() {
        ServletRequestAttributes attributes=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request=attributes==null?null:attributes.getRequest(); return IpUtils.getIpAddr(request,securityProperties.isTrustForwardedHeaders());
    }
    private void logLogin(String type,Long userId,String account,String loginType,boolean success,String message) {
        try { LoginLogEntity entity=new LoginLogEntity(); entity.setUserType(type); entity.setUserId(userId); entity.setAccount(account); entity.setLoginType(loginType);
            entity.setIp(ip()); entity.setLocation(locationService.locate(ip())); entity.setSuccess(success); entity.setMessage(message); entity.setTraceId(MDC.get("traceId")); entity.setCreateTime(LocalDateTime.now()); loginLogMapper.insert(entity);
        } catch (Exception e) { log.warn("Unable to persist login log",e); }
    }
}
