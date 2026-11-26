package com.heima.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.RefreshTokenMapper;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.auth.mapper.UserProfileMapper;
import com.heima.modules.system.dto.SystemDtos.*;
import com.heima.modules.system.entity.*;
import com.heima.modules.system.mapper.*;
import com.heima.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemService {
    private final SysAdminMapper adminMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final UserAccountMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminMeView adminMe() {
        Long id=UserUtil.requireAdmin().getId(); SysAdmin admin=adminMapper.selectById(id); List<SysPermission> all=adminMapper.selectPermissions(id);
        List<String> permissions=all.stream().map(SysPermission::getCode).toList();
        return new AdminMeView(adminView(admin),permissions,menuTree(all));
    }

    public PageResult<AdminView> admins(long page,long size,String keyword,Integer status) {
        Page<SysAdmin> result=adminMapper.selectPage(page(page,size),new LambdaQueryWrapper<SysAdmin>()
                .and(keyword!=null&&!keyword.isBlank(),w->w.like(SysAdmin::getUsername,keyword).or().like(SysAdmin::getNickname,keyword))
                .eq(status!=null,SysAdmin::getStatus,status).orderByDesc(SysAdmin::getCreateTime));
        return PageResult.of(result.getRecords().stream().map(this::adminView).toList(),result.getTotal(),result.getCurrent(),result.getSize());
    }

    @Transactional public AdminView createAdmin(AdminCreateRequest request) {
        if (adminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername,request.username()))>0) throw new ServiceException(409,"管理员用户名已存在");
        SysAdmin admin=new SysAdmin(); admin.setUsername(request.username()); admin.setNickname(request.nickname()); admin.setEmail(request.email());
        admin.setPassword(passwordEncoder.encode(request.password())); admin.setStatus(1); admin.setTokenVersion(0); adminMapper.insert(admin);
        if (request.roleIds()!=null&&!request.roleIds().isEmpty()) assignAdminRoles(admin.getId(),request.roleIds()); return adminView(admin);
    }
    public AdminView updateAdmin(Long id,AdminUpdateRequest request) {
        SysAdmin admin=requiredAdmin(id); admin.setNickname(request.nickname()); admin.setEmail(request.email()); adminMapper.updateById(admin); return adminView(admin);
    }
    public void adminStatus(Long id,int status) {
        if (id.equals(UserUtil.requireAdmin().getId())&&status==0) throw new ServiceException(400,"不能停用当前管理员");
        if (adminMapper.updateStatusAndVersion(id,status)!=1) throw new ServiceException(404,"管理员不存在");
    }
    @Transactional public void deleteAdmin(Long id) {
        if (id.equals(UserUtil.requireAdmin().getId())) throw new ServiceException(400,"不能删除当前管理员");
        requiredAdmin(id);
        if (adminMapper.hasBusinessReferences(id)) throw new ServiceException(409,"管理员存在审核或运营业务数据，不能删除，请改用停用");
        adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getAdminId,id));
        refreshTokenMapper.deleteAll("ADMIN",id);
        if (adminMapper.deleteById(id)!=1) throw new ServiceException(404,"管理员不存在");
    }
    @Transactional public void assignAdminRoles(Long id,List<Long> roleIds) {
        requiredAdmin(id); requireRoles(roleIds); adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getAdminId,id));
        for (Long roleId:new LinkedHashSet<>(roleIds)) { SysAdminRole relation=new SysAdminRole(); relation.setAdminId(id); relation.setRoleId(roleId); relation.setCreateTime(LocalDateTime.now()); adminRoleMapper.insert(relation); }
        adminMapper.incrementTokenVersion(id);
    }

    public PageResult<SysRole> roles(long page,long size,String keyword,Integer status) {
        Page<SysRole> result=roleMapper.selectPage(page(page,size),new LambdaQueryWrapper<SysRole>().like(keyword!=null&&!keyword.isBlank(),SysRole::getName,keyword)
                .eq(status!=null,SysRole::getStatus,status).orderByDesc(SysRole::getCreateTime)); return PageResult.of(result);
    }
    public SysRole createRole(RoleRequest request) {
        checkRoleCode(request.code(),null); SysRole role=new SysRole(); role.setName(request.name()); role.setCode(request.code()); role.setDescription(request.description()); role.setStatus(1); roleMapper.insert(role); return role;
    }
    public SysRole updateRole(Long id,RoleRequest request) {
        SysRole role=requiredRole(id); checkRoleCode(request.code(),id); role.setName(request.name()); role.setCode(request.code()); role.setDescription(request.description()); roleMapper.updateById(role); return role;
    }
    public void roleStatus(Long id,int status) { SysRole role=requiredRole(id); role.setStatus(status); roleMapper.updateById(role); }
    @Transactional public void deleteRole(Long id) {
        if (id==1L) throw new ServiceException(400,"内置超级管理员角色不可删除"); requiredRole(id);
        adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getRoleId,id));
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId,id)); roleMapper.deleteById(id);
    }
    public List<Long> rolePermissionIds(Long id) {
        requiredRole(id); return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId,id)).stream().map(SysRolePermission::getPermissionId).toList();
    }
    @Transactional public void assignRolePermissions(Long id,List<Long> permissionIds) {
        requiredRole(id); if (permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId,permissionIds))!=new HashSet<>(permissionIds).size()) throw new ServiceException(400,"包含不存在的权限");
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId,id));
        for (Long permissionId:new LinkedHashSet<>(permissionIds)) { SysRolePermission relation=new SysRolePermission(); relation.setRoleId(id); relation.setPermissionId(permissionId); relation.setCreateTime(LocalDateTime.now()); rolePermissionMapper.insert(relation); }
    }

    public PageResult<SysPermission> permissions(long page,long size,String keyword,String type) {
        Page<SysPermission> result=permissionMapper.selectPage(page(page,size),new LambdaQueryWrapper<SysPermission>()
                .and(keyword!=null&&!keyword.isBlank(),w->w.like(SysPermission::getName,keyword).or().like(SysPermission::getCode,keyword))
                .eq(type!=null&&!type.isBlank(),SysPermission::getType,type).orderByAsc(SysPermission::getSortOrder)); return PageResult.of(result);
    }
    public SysPermission createPermission(PermissionRequest request) {
        checkPermissionCode(request.code(),null); SysPermission p=new SysPermission(); fillPermission(p,request); p.setStatus(1); permissionMapper.insert(p); return p;
    }
    public SysPermission updatePermission(Long id,PermissionRequest request) {
        SysPermission p=permissionMapper.selectById(id); if (p==null) throw new ServiceException(404,"权限不存在"); checkPermissionCode(request.code(),id); fillPermission(p,request); permissionMapper.updateById(p); return p;
    }
    public void permissionStatus(Long id,int status) { SysPermission p=permissionMapper.selectById(id); if (p==null) throw new ServiceException(404,"权限不存在"); p.setStatus(status); permissionMapper.updateById(p); }
    @Transactional public void deletePermission(Long id) {
        if (permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId,id))>0) throw new ServiceException(400,"请先删除子权限");
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId,id)); permissionMapper.deleteById(id);
    }

    public PageResult<UserView> users(long page,long size,String keyword,Integer status) {
        Page<UserAccount> result=userMapper.selectPage(page(page,size),new LambdaQueryWrapper<UserAccount>()
                .and(keyword!=null&&!keyword.isBlank(),w->w.like(UserAccount::getUsername,keyword).or().like(UserAccount::getEmail,keyword).or().like(UserAccount::getPhone,keyword))
                .eq(status!=null,UserAccount::getStatus,status).orderByDesc(UserAccount::getCreateTime));
        List<Long> ids=result.getRecords().stream().map(UserAccount::getId).toList(); Map<Long,UserProfile> profiles=ids.isEmpty()?Map.of():profileMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(UserProfile::getUserId,Function.identity()));
        List<UserView> views=result.getRecords().stream().map(u->{ UserProfile p=profiles.get(u.getId()); return new UserView(u.getId(),u.getUsername(),u.getEmail(),u.getPhone(),p==null?u.getUsername():p.getNickname(),u.getStatus(),u.getCreateTime()); }).toList();
        return PageResult.of(views,result.getTotal(),result.getCurrent(),result.getSize());
    }
    @Transactional public UserView createUser(UserCreateRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUsername,request.username()))>0) throw new ServiceException(409,"用户名已存在");
        checkUserContacts(null,request.email(),request.phone());
        UserAccount user=new UserAccount(); user.setUsername(request.username()); user.setEmail(request.email()); user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password())); user.setStatus(1); user.setTokenVersion(0); userMapper.insert(user);
        UserProfile profile=new UserProfile(); profile.setUserId(user.getId()); profile.setNickname(request.nickname()); profile.setGender(0); profile.setCreateTime(LocalDateTime.now()); profile.setUpdateTime(LocalDateTime.now()); profileMapper.insert(profile);
        return new UserView(user.getId(),user.getUsername(),user.getEmail(),user.getPhone(),profile.getNickname(),user.getStatus(),user.getCreateTime());
    }
    @Transactional public UserView updateUser(Long id,UserUpdateRequest request) {
        UserAccount user=userMapper.selectById(id); if (user==null) throw new ServiceException(404,"用户不存在"); checkUserContacts(id,request.email(),request.phone());
        user.setEmail(request.email()); user.setPhone(request.phone()); userMapper.updateById(user); UserProfile profile=profileMapper.selectById(id); profile.setNickname(request.nickname()); profile.setUpdateTime(LocalDateTime.now()); profileMapper.updateById(profile);
        return new UserView(user.getId(),user.getUsername(),user.getEmail(),user.getPhone(),profile.getNickname(),user.getStatus(),user.getCreateTime());
    }
    public void userStatus(Long id,int status) { if (userMapper.updateStatusAndVersion(id,status)!=1) throw new ServiceException(404,"用户不存在"); }
    @Transactional public void deleteUser(Long id) {
        if (userMapper.selectById(id)==null) throw new ServiceException(404,"用户不存在");
        if (userMapper.hasBusinessReferences(id)) throw new ServiceException(409,"用户存在内容、互动、积分或交易数据，不能删除，请改用停用");
        profileMapper.deleteById(id);
        refreshTokenMapper.deleteAll("USER",id);
        if (userMapper.deleteById(id)!=1) throw new ServiceException(404,"用户不存在");
    }

    public PageResult<OperationLogEntity> operationLogs(long page,long size,String keyword,String module,Boolean success) {
        Page<OperationLogEntity> result=operationLogMapper.selectPage(page(page,size),new LambdaQueryWrapper<OperationLogEntity>()
                .and(keyword!=null&&!keyword.isBlank(),w->w.like(OperationLogEntity::getOperatorName,keyword).or().like(OperationLogEntity::getContent,keyword).or().like(OperationLogEntity::getTraceId,keyword))
                .eq(module!=null&&!module.isBlank(),OperationLogEntity::getModule,module).eq(success!=null,OperationLogEntity::getSuccess,success).orderByDesc(OperationLogEntity::getCreateTime));
        return PageResult.of(result);
    }

    private void checkUserContacts(Long excludeId,String email,String phone) {
        if (email!=null&&!email.isBlank()&&userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail,email).ne(excludeId!=null,UserAccount::getId,excludeId))>0) throw new ServiceException(409,"邮箱已被使用");
        if (phone!=null&&!phone.isBlank()&&userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getPhone,phone).ne(excludeId!=null,UserAccount::getId,excludeId))>0) throw new ServiceException(409,"手机号已被使用");
    }
    private AdminView adminView(SysAdmin admin) { return new AdminView(admin.getId(),admin.getUsername(),admin.getNickname(),admin.getEmail(),admin.getStatus(),adminMapper.selectRoleIds(admin.getId()),admin.getCreateTime()); }
    private SysAdmin requiredAdmin(Long id) { SysAdmin admin=adminMapper.selectById(id); if (admin==null) throw new ServiceException(404,"管理员不存在"); return admin; }
    private SysRole requiredRole(Long id) { SysRole role=roleMapper.selectById(id); if (role==null) throw new ServiceException(404,"角色不存在"); return role; }
    private void requireRoles(List<Long> ids) { if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().in(SysRole::getId,ids))!=new HashSet<>(ids).size()) throw new ServiceException(400,"包含不存在的角色"); }
    private void checkRoleCode(String code,Long exclude) { if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode,code).ne(exclude!=null,SysRole::getId,exclude))>0) throw new ServiceException(409,"角色编码已存在"); }
    private void checkPermissionCode(String code,Long exclude) { if (permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode,code).ne(exclude!=null,SysPermission::getId,exclude))>0) throw new ServiceException(409,"权限编码已存在"); }
    private void fillPermission(SysPermission p,PermissionRequest r) { p.setParentId(r.parentId()); p.setName(r.name()); p.setCode(r.code()); p.setType(r.type()); p.setPath(r.path()); p.setIcon(r.icon()); p.setSortOrder(r.sortOrder()); }
    private <T> Page<T> page(long page,long size) { return new Page<>(Math.max(1,page),Math.min(100,Math.max(1,size))); }
    private List<MenuView> menuTree(List<SysPermission> permissions) {
        Map<Long,List<SysPermission>> children=permissions.stream().filter(p->!"BUTTON".equals(p.getType())).collect(Collectors.groupingBy(SysPermission::getParentId));
        return buildMenus(0L,children);
    }
    private List<MenuView> buildMenus(Long parent,Map<Long,List<SysPermission>> children) {
        return children.getOrDefault(parent,List.of()).stream().sorted(Comparator.comparing(SysPermission::getSortOrder).thenComparing(SysPermission::getId))
                .map(p->new MenuView(p.getId(),p.getName(),p.getCode(),p.getPath(),p.getIcon(),buildMenus(p.getId(),children))).toList();
    }
}
