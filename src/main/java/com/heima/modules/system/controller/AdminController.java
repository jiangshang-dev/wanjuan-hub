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

@Tag(name="RBAC-管理员")
@RestController
@RequestMapping("/api/admin/system/admins")
@RequiredArgsConstructor
public class AdminController {
    private final SystemService service;
    @Operation(summary="管理员分页") @PreAuthorize("hasAuthority('system:admin:list')") @GetMapping public R<PageResult<AdminView>> page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="10") long size,@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer status) { return R.ok(service.admins(page,size,keyword,status)); }
    @Operation(summary="新增管理员") @OperationLog(module="管理员",value="新增管理员") @PreAuthorize("hasAuthority('system:admin:create')") @PostMapping public R<AdminView> create(@Valid @RequestBody AdminCreateRequest request) { return R.ok(service.createAdmin(request)); }
    @Operation(summary="修改管理员") @OperationLog(module="管理员",value="修改管理员",targetId="#id") @PreAuthorize("hasAuthority('system:admin:update')") @PutMapping("/{id}") public R<AdminView> update(@PathVariable Long id,@Valid @RequestBody AdminUpdateRequest request) { return R.ok(service.updateAdmin(id,request)); }
    @Operation(summary="修改管理员状态") @OperationLog(module="管理员",value="修改管理员状态",targetId="#id") @PreAuthorize("hasAuthority('system:admin:status')") @PutMapping("/{id}/status") public R<Void> status(@PathVariable Long id,@Valid @RequestBody StatusRequest request) { service.adminStatus(id,request.status()); return R.ok(); }
    @Operation(summary="分配管理员角色") @OperationLog(module="管理员",value="分配角色",targetId="#id") @PreAuthorize("hasAuthority('system:admin:assign-role')") @PutMapping("/{id}/roles") public R<Void> roles(@PathVariable Long id,@Valid @RequestBody IdsRequest request) { service.assignAdminRoles(id,request.ids()); return R.ok(); }
    @Operation(summary="删除管理员") @OperationLog(module="管理员",value="删除管理员",targetId="#id") @PreAuthorize("hasAuthority('system:admin:delete')") @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id) { service.deleteAdmin(id); return R.ok(); }
}
