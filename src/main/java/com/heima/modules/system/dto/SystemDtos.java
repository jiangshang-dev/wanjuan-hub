package com.heima.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class SystemDtos {
    private SystemDtos() {}

    @Schema(description = "管理员新增请求")
    public record AdminCreateRequest(
            @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min=3,max=50) String username,
            @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max=50) String nickname,
            @Schema(description = "邮箱") @Email String email,
            @Schema(description = "初始密码", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min=8,max=72) String password,
            @Schema(description = "角色ID列表") List<Long> roleIds) {}

    @Schema(description = "管理员修改请求")
    public record AdminUpdateRequest(
            @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max=50) String nickname,
            @Schema(description = "邮箱") @Email String email) {}

    @Schema(description = "角色保存请求")
    public record RoleRequest(
            @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max=50) String name,
            @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max=50) String code,
            @Schema(description = "描述") @Size(max=255) String description) {}

    @Schema(description = "权限保存请求")
    public record PermissionRequest(
            @Schema(description = "父权限ID，根节点为0") @NotNull @Min(0) Long parentId,
            @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max=50) String name,
            @Schema(description = "权限编码", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max=100) String code,
            @Schema(description = "类型：DIRECTORY/MENU/BUTTON", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Pattern(regexp="DIRECTORY|MENU|BUTTON") String type,
            @Schema(description = "路由路径") @Size(max=255) String path,
            @Schema(description = "图标") @Size(max=100) String icon,
            @Schema(description = "排序") @NotNull Integer sortOrder) {}

    @Schema(description = "状态修改请求")
    public record StatusRequest(@Schema(description = "状态：1启用/0停用", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Min(0) @Max(1) Integer status) {}

    @Schema(description = "ID集合请求")
    public record IdsRequest(@Schema(description = "ID集合", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty List<Long> ids) {}

    @Schema(description = "管理员视图")
    public record AdminView(@Schema(description="ID") Long id, @Schema(description="用户名") String username,
            @Schema(description="昵称") String nickname, @Schema(description="邮箱") String email,
            @Schema(description="状态") Integer status, @Schema(description="角色ID") List<Long> roleIds,
            @Schema(description="创建时间") LocalDateTime createTime) {}

    @Schema(description = "菜单节点")
    public record MenuView(@Schema(description="权限ID") Long id, @Schema(description="名称") String name,
            @Schema(description="编码") String code, @Schema(description="路径") String path,
            @Schema(description="图标") String icon, @Schema(description="子菜单") List<MenuView> children) {}

    @Schema(description = "后台新增用户请求")
    public record UserCreateRequest(@Schema(description="用户名") @NotBlank @Size(min=3,max=50) String username,
            @Schema(description="邮箱") @Email String email, @Schema(description="手机号") @Pattern(regexp="^1[3-9]\\d{9}$") String phone,
            @Schema(description="初始密码") @NotBlank @Size(min=8,max=72) String password,
            @Schema(description="昵称") @NotBlank @Size(max=50) String nickname) {}

    @Schema(description = "后台修改用户请求")
    public record UserUpdateRequest(@Schema(description="邮箱") @Email String email,
            @Schema(description="手机号") @Pattern(regexp="^1[3-9]\\d{9}$") String phone,
            @Schema(description="昵称") @NotBlank @Size(max=50) String nickname) {}

    @Schema(description = "前台用户管理视图")
    public record UserView(@Schema(description="ID") Long id, @Schema(description="用户名") String username,
            @Schema(description="邮箱") String email, @Schema(description="手机号") String phone,
            @Schema(description="昵称") String nickname, @Schema(description="状态") Integer status,
            @Schema(description="创建时间") LocalDateTime createTime) {}

    @Schema(description = "当前管理员")
    public record AdminMeView(@Schema(description="管理员信息") AdminView admin,
            @Schema(description="按钮和接口权限码") List<String> permissions,
            @Schema(description="菜单树") List<MenuView> menus) {}
}
