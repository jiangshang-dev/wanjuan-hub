package com.heima.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthDtos {
    private AuthDtos() {}
    @Schema(description="邮箱验证码请求") public record EmailCodeRequest(@Schema(description="场景：REGISTER/LOGIN/RESET/BIND") @NotBlank String scene, @Schema(description="邮箱") @NotBlank @Email String email, @Schema(description="图形验证码ID") @NotBlank String captchaId, @Schema(description="图形验证码") @NotBlank String captchaCode) {}
    @Schema(description="短信验证码请求") public record SmsCodeRequest(@Schema(description="场景：LOGIN/BIND") @NotBlank String scene, @Schema(description="手机号") @NotBlank @Pattern(regexp="^1[3-9]\\d{9}$",message="手机号格式错误") String phone, @Schema(description="图形验证码ID") @NotBlank String captchaId, @Schema(description="图形验证码") @NotBlank String captchaCode) {}
    @Schema(description="注册请求") public record RegisterRequest(@Schema(description="用户名") @NotBlank @Size(min=3,max=50) String username, @Schema(description="邮箱") @NotBlank @Email String email, @Schema(description="密码") @NotBlank @Size(min=8,max=72) String password, @Schema(description="邮箱验证码") @NotBlank String emailCode, @Schema(description="已同意用户协议和隐私政策") @AssertTrue(message="请阅读并同意用户协议和隐私政策") boolean agreementAccepted) {}
    @Schema(description="账号密码登录请求") public record LoginRequest(@Schema(description="用户名、邮箱或手机号") @NotBlank String account, @Schema(description="密码") @NotBlank String password, @Schema(description="图形验证码ID") @NotBlank String captchaId, @Schema(description="图形验证码") @NotBlank String captchaCode) {}
    @Schema(description="邮箱验证码登录请求") public record EmailLoginRequest(@Schema(description="邮箱") @NotBlank @Email String email, @Schema(description="邮箱验证码") @NotBlank String emailCode, @Schema(description="图形验证码ID") @NotBlank String captchaId, @Schema(description="图形验证码") @NotBlank String captchaCode) {}
    @Schema(description="手机验证码登录请求") public record PhoneLoginRequest(@Schema(description="手机号") @NotBlank String phone, @Schema(description="短信验证码") @NotBlank String smsCode, @Schema(description="图形验证码ID") @NotBlank String captchaId, @Schema(description="图形验证码") @NotBlank String captchaCode) {}
    @Schema(description="刷新令牌请求") public record RefreshRequest(@Schema(description="刷新令牌") @NotBlank String refreshToken) {}
    @Schema(description="退出请求") public record LogoutRequest(@Schema(description="刷新令牌") @NotBlank String refreshToken) {}
    @Schema(description="重置密码请求") public record ResetPasswordRequest(@Schema(description="邮箱") @NotBlank @Email String email, @Schema(description="邮箱验证码") @NotBlank String emailCode, @Schema(description="新密码") @NotBlank @Size(min=8,max=72) String newPassword) {}
    @Schema(description="图形验证码") public record CaptchaResponse(@Schema(description="验证码ID") String captchaId,@Schema(description="Base64 PNG 图片，含 data URI 前缀") String image) {}
    @Schema(description="登录令牌") public record TokenResponse(@Schema(description="访问令牌") String accessToken,@Schema(description="刷新令牌") String refreshToken,@Schema(description="令牌类型") String tokenType,@Schema(description="访问令牌有效秒数") long expiresIn) {}
    @Schema(description="当前前台用户") public record MeResponse(
            @Schema(description="用户ID") Long id, @Schema(description="用户名") String username,
            @Schema(description="邮箱") String email, @Schema(description="手机号") String phone,
            @Schema(description="当前公开昵称") String nickname, @Schema(description="当前公开头像短时效地址") String avatar,
            @Schema(description="当前公开头像文件ID") Long avatarFileId, @Schema(description="当前公开签名") String signature,
            @Schema(description="生日") LocalDate birthday, @Schema(description="性别：0未知/1男/2女") Integer gender,
            @Schema(description="待审核昵称") String pendingNickname, @Schema(description="待审核头像短时效地址") String pendingAvatar,
            @Schema(description="待审核头像文件ID") Long pendingAvatarFileId, @Schema(description="待审核签名") String pendingSignature,
            @Schema(description="是否待审核移除头像") Boolean pendingRemoveAvatar,
            @Schema(description="资料审核状态") String profileAuditStatus, @Schema(description="资料审核原因") String profileAuditReason) {}
}
