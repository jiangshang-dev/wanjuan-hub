package com.heima.modules.auth.controller;

import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.auth.dto.AuthDtos.*;
import com.heima.modules.auth.service.AuthService;
import com.heima.modules.system.dto.SystemDtos.AdminMeView;
import com.heima.modules.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name="后台认证")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService authService;
    private final SystemService systemService;
    @Operation(summary="管理员登录") @OperationLog(module="后台认证",value="管理员登录") @PostMapping("/login") public R<TokenResponse> login(@Valid @RequestBody LoginRequest request) { return R.ok(authService.adminLogin(request)); }
    @Operation(summary="刷新后台令牌") @PostMapping("/refresh") public R<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) { return R.ok(authService.refresh(request,"ADMIN")); }
    @Operation(summary="管理员退出") @PostMapping("/logout") public R<Void> logout(@Valid @RequestBody LogoutRequest request) { authService.logout(request,"ADMIN"); return R.ok(); }
    @Operation(summary="当前管理员、权限和菜单") @GetMapping("/me") public R<AdminMeView> me() { return R.ok(systemService.adminMe()); }
}
