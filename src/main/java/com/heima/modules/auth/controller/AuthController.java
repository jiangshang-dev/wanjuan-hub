package com.heima.modules.auth.controller;

import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.auth.dto.AuthDtos.*;
import com.heima.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name="前台认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    @Operation(summary="生成图形验证码") @PostMapping("/captcha") public R<CaptchaResponse> captcha() { return R.ok(service.captcha()); }
    @Operation(summary="发送邮箱验证码") @PostMapping("/code") public R<Void> emailCode(@Valid @RequestBody EmailCodeRequest request) { service.sendEmailCode(request); return R.ok(); }
    @Operation(summary="发送短信验证码") @PostMapping("/sms-code") public R<Void> smsCode(@Valid @RequestBody SmsCodeRequest request) { service.sendSmsCode(request); return R.ok(); }
    @Operation(summary="用户注册") @OperationLog(module="认证",value="用户注册") @PostMapping("/register") public R<Void> register(@Valid @RequestBody RegisterRequest request) { service.register(request); return R.ok(); }
    @Operation(summary="账号密码登录") @OperationLog(module="认证",value="账号密码登录") @PostMapping("/login") public R<TokenResponse> login(@Valid @RequestBody LoginRequest request) { return R.ok(service.login(request)); }
    @Operation(summary="邮箱验证码登录") @PostMapping("/login/email") public R<TokenResponse> emailLogin(@Valid @RequestBody EmailLoginRequest request) { return R.ok(service.loginByEmail(request)); }
    @Operation(summary="手机验证码登录") @PostMapping("/login/phone") public R<TokenResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) { return R.ok(service.loginByPhone(request)); }
    @Operation(summary="刷新前台令牌") @PostMapping("/refresh") public R<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) { return R.ok(service.refresh(request,"USER")); }
    @Operation(summary="退出登录") @PostMapping("/logout") public R<Void> logout(@Valid @RequestBody LogoutRequest request) { service.logout(request,"USER"); return R.ok(); }
    @Operation(summary="邮箱验证码重置密码") @PostMapping("/password/reset") public R<Void> reset(@Valid @RequestBody ResetPasswordRequest request) { service.resetPassword(request); return R.ok(); }
    @Operation(summary="当前用户") @GetMapping("/me") public R<MeResponse> me() { return R.ok(service.me()); }
}
