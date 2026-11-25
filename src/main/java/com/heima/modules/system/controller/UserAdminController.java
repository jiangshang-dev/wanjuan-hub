package com.heima.modules.system.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.system.dto.SystemDtos.*;
import com.heima.modules.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name="RBAC-前台用户")
@RestController
@RequestMapping("/api/admin/system/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final SystemService service;
    @Operation(summary="用户分页") @PreAuthorize("hasAuthority('system:user:list')") @GetMapping public R<PageResult<UserView>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer status) { return R.ok(service.users(page,size,keyword,status)); }
    @Operation(summary="新增用户") @OperationLog(module="用户管理",value="新增用户") @PreAuthorize("hasAuthority('system:user:create')") @PostMapping public R<UserView> create(@Valid @RequestBody UserCreateRequest request) { return R.ok(service.createUser(request)); }
    @Operation(summary="修改用户") @OperationLog(module="用户管理",value="修改用户",targetId="#id") @PreAuthorize("hasAuthority('system:user:update')") @PutMapping("/{id}") public R<UserView> update(@PathVariable Long id,@Valid @RequestBody UserUpdateRequest request) { return R.ok(service.updateUser(id,request)); }
    @Operation(summary="修改用户状态") @OperationLog(module="用户管理",value="修改用户状态",targetId="#id") @PreAuthorize("hasAuthority('system:user:status')") @PutMapping("/{id}/status") public R<Void> status(@PathVariable Long id,@Valid @RequestBody StatusRequest request) { service.userStatus(id,request.status()); return R.ok(); }
    @Operation(summary="删除用户") @OperationLog(module="用户管理",value="删除用户",targetId="#id") @PreAuthorize("hasAuthority('system:user:delete')") @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id) { service.deleteUser(id); return R.ok(); }
}
