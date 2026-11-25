package com.heima.modules.system.controller;

import com.heima.common.PageResult;
import com.heima.common.R;
import com.heima.common.annotation.OperationLog;
import com.heima.modules.system.dto.SystemDtos.*;
import com.heima.modules.system.entity.SysPermission;
import com.heima.modules.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name="RBAC-权限")
@RestController
@RequestMapping("/api/admin/system/permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final SystemService service;
    @Operation(summary="权限分页") @PreAuthorize("hasAuthority('system:permission:list')") @GetMapping public R<PageResult<SysPermission>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="100") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) String type) { return R.ok(service.permissions(page,size,keyword,type)); }
    @Operation(summary="新增权限") @OperationLog(module="权限",value="新增权限") @PreAuthorize("hasAuthority('system:permission:create')") @PostMapping public R<SysPermission> create(@Valid @RequestBody PermissionRequest request) { return R.ok(service.createPermission(request)); }
    @Operation(summary="修改权限") @OperationLog(module="权限",value="修改权限",targetId="#id") @PreAuthorize("hasAuthority('system:permission:update')") @PutMapping("/{id}") public R<SysPermission> update(@PathVariable Long id,@Valid @RequestBody PermissionRequest request) { return R.ok(service.updatePermission(id,request)); }
    @Operation(summary="修改权限状态") @OperationLog(module="权限",value="修改权限状态",targetId="#id") @PreAuthorize("hasAuthority('system:permission:status')") @PutMapping("/{id}/status") public R<Void> status(@PathVariable Long id,@Valid @RequestBody StatusRequest request) { service.permissionStatus(id,request.status()); return R.ok(); }
    @Operation(summary="删除权限") @OperationLog(module="权限",value="删除权限",targetId="#id") @PreAuthorize("hasAuthority('system:permission:delete')") @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id) { service.deletePermission(id); return R.ok(); }
}
