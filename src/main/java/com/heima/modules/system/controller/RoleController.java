package com.heima.modules.system.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.system.dto.SystemDtos.*;
import com.heima.modules.system.entity.SysRole;
import com.heima.modules.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name="RBAC-角色")
@RestController
@RequestMapping("/api/admin/system/roles")
@RequiredArgsConstructor
public class RoleController {
    private final SystemService service;
    @Operation(summary="角色分页") @PreAuthorize("hasAuthority('system:role:list')") @GetMapping public R<PageResult<SysRole>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer status) { return R.ok(service.roles(page,size,keyword,status)); }
    @Operation(summary="新增角色") @OperationLog(module="角色",value="新增角色") @PreAuthorize("hasAuthority('system:role:create')") @PostMapping public R<SysRole> create(@Valid @RequestBody RoleRequest request) { return R.ok(service.createRole(request)); }
    @Operation(summary="修改角色") @OperationLog(module="角色",value="修改角色",targetId="#id") @PreAuthorize("hasAuthority('system:role:update')") @PutMapping("/{id}") public R<SysRole> update(@PathVariable Long id,@Valid @RequestBody RoleRequest request) { return R.ok(service.updateRole(id,request)); }
    @Operation(summary="修改角色状态") @OperationLog(module="角色",value="修改角色状态",targetId="#id") @PreAuthorize("hasAuthority('system:role:status')") @PutMapping("/{id}/status") public R<Void> status(@PathVariable Long id,@Valid @RequestBody StatusRequest request) { service.roleStatus(id,request.status()); return R.ok(); }
    @Operation(summary="查询角色权限ID") @PreAuthorize("hasAuthority('system:role:list')") @GetMapping("/{id}/permissions") public R<java.util.List<Long>> permissionIds(@PathVariable Long id) { return R.ok(service.rolePermissionIds(id)); }
    @Operation(summary="分配角色权限") @OperationLog(module="角色",value="分配权限",targetId="#id") @PreAuthorize("hasAuthority('system:role:assign-permission')") @PutMapping("/{id}/permissions") public R<Void> permissions(@PathVariable Long id,@Valid @RequestBody IdsRequest request) { service.assignRolePermissions(id,request.ids()); return R.ok(); }
    @Operation(summary="删除角色") @OperationLog(module="角色",value="删除角色",targetId="#id") @PreAuthorize("hasAuthority('system:role:delete')") @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id) { service.deleteRole(id); return R.ok(); }
}
