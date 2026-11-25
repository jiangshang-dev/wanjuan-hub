package com.heima.modules.user.controller;

import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.auth.dto.AuthDtos.MeResponse;
import com.heima.modules.user.dto.UserDtos.*;
import com.heima.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name="前台用户中心")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    @Operation(summary="获取个人资料") @GetMapping("/profile") public R<MeResponse> profile() { return R.ok(service.profile()); }
    @Operation(summary="修改个人资料") @PutMapping("/profile") public R<MeResponse> update(@Valid @RequestBody ProfileRequest request) { return R.ok(service.updateProfile(request)); }
    @Operation(summary="修改密码") @OperationLog(module="用户中心",value="修改密码") @PostMapping("/password") public R<Void> password(@Valid @RequestBody PasswordRequest request) { service.changePassword(request); return R.ok(); }
    @Operation(summary="绑定邮箱") @OperationLog(module="用户中心",value="绑定邮箱") @PostMapping("/email/bind") public R<Void> email(@Valid @RequestBody BindEmailRequest request) { service.bindEmail(request); return R.ok(); }
    @Operation(summary="绑定手机") @OperationLog(module="用户中心",value="绑定手机") @PostMapping("/phone/bind") public R<Void> phone(@Valid @RequestBody BindPhoneRequest request) { service.bindPhone(request); return R.ok(); }
}
